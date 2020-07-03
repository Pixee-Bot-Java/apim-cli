package com.axway.apim.orgexport;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.adapter.APIManagerAdapter;
import com.axway.apim.api.model.Organization;
import com.axway.apim.cli.APIMCLIServiceProvider;
import com.axway.apim.cli.CLIServiceMethod;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.errorHandling.ErrorCode;
import com.axway.apim.lib.errorHandling.ErrorCodeMapper;
import com.axway.apim.lib.errorHandling.ErrorState;
import com.axway.apim.lib.utils.rest.APIMHttpClient;
import com.axway.apim.orgexport.impl.OrganizationExporter;
import com.axway.apim.orgexport.impl.OrganizationExporter.ExportImpl;
import com.axway.apim.orgexport.lib.OrgExportCLIOptions;
import com.axway.apim.orgexport.lib.OrgExportParams;

public class OrganizationExportApp implements APIMCLIServiceProvider {

	private static Logger LOG = LoggerFactory.getLogger(OrganizationExportApp.class);

	static ErrorCodeMapper errorCodeMapper = new ErrorCodeMapper();
	static ErrorState errorState = ErrorState.getInstance();

	@Override
	public String getName() {
		return "Organization - E X P O R T / U T I L S";
	}

	@Override
	public String getVersion() {
		return OrganizationExportApp.class.getPackage().getImplementationVersion();
	}

	@Override
	public String getGroupId() {
		return "org";
	}

	@Override
	public String getGroupDescription() {
		return "Manage your organizations";
	}
	
	@CLIServiceMethod(name = "get", description = "Get Organizations from the API-Manager in different formats")
	public static int export(String args[]) {
		try {
			OrgExportParams params = new OrgExportParams(new OrgExportCLIOptions(args));
			switch(params.getOutputFormat()) {
			case console:
				return runExport(params, ExportImpl.CONSOLE_EXPORTER);
			case json:
				return runExport(params, ExportImpl.JSON_EXPORTER);
			default:
				return runExport(params, ExportImpl.CONSOLE_EXPORTER);
			}
		} catch (AppException e) {
			
			if(errorState.hasError()) {
				errorState.logErrorMessages(LOG);
				if(errorState.isLogStackTrace()) LOG.error(e.getMessage(), e);
				return new ErrorCodeMapper().getMapedErrorCode(errorState.getErrorCode()).getCode();
			} else {
				LOG.error(e.getMessage(), e);
				return new ErrorCodeMapper().getMapedErrorCode(e.getErrorCode()).getCode();
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return ErrorCode.UNXPECTED_ERROR.getCode();
		}
	}

	private static int runExport(OrgExportParams params, ExportImpl exportImpl) {
		try {
			// We need to clean some Singleton-Instances, as tests are running in the same JVM
			APIManagerAdapter.deleteInstance();
			ErrorState.deleteInstance();
			APIMHttpClient.deleteInstance();
			
			APIManagerAdapter adapter = APIManagerAdapter.getInstance();

			OrganizationExporter exporter = OrganizationExporter.create(exportImpl, params);
			List<Organization> orgs = adapter.orgAdapter.getOrgs(exporter.getFilter());
			if(orgs.size()==0) {
				if(LOG.isDebugEnabled()) {
					LOG.info("No organizations found using filter: " + exporter.getFilter());
				} else {
					LOG.info("No organizations found based on the given criteria.");
				}
			} else {
				LOG.info("Found " + orgs.size() + " organization(s).");
				
				exporter.export(orgs);
				if(exporter.hasError()) {
					LOG.info("");
					LOG.error("Please check the log. At least one error was recorded.");
				} else {
					LOG.debug("Successfully exported " + orgs.size() + " organization(s).");
				}
			}
			APIManagerAdapter.deleteInstance();
		} catch (AppException ap) { 
			ErrorState errorState = ErrorState.getInstance();
			if(errorState.hasError()) {
				errorState.logErrorMessages(LOG);
				if(errorState.isLogStackTrace()) LOG.error(ap.getMessage(), ap);
				return errorCodeMapper.getMapedErrorCode(errorState.getErrorCode()).getCode();
			} else {
				LOG.error(ap.getMessage(), ap);
				return errorCodeMapper.getMapedErrorCode(ap.getErrorCode()).getCode();
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return ErrorCode.UNXPECTED_ERROR.getCode();
		}
		return ErrorState.getInstance().getErrorCode().getCode();
	}

	public static void main(String args[]) { 
		int rc = export(args);
		System.exit(rc);
	}


}
