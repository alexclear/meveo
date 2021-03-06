package org.meveo.admin.ftp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.service.admin.impl.UserService;
import org.slf4j.Logger;

/**
 * 
 * @author Tyshan Shi
 *
 */
@Startup
@Singleton
public class MeveoMinaFTPListener {
	@Inject
	private Logger log;
	@Inject
	private UserService userService;
	
	@Inject
	private MeveoDefaultFtplet meveoDefaultFtplet;
	
	private ParamBean paramBean=ParamBean.getInstance();
	
	
	private FtpServer server = null;

	@PostConstruct
	private void init() throws FtpException {
		String portStr=paramBean.getProperty("ftpserver.port", null);
		if(StringUtils.isBlank(portStr)){return;}
		Integer port=null;
		try{
			port=Integer.parseInt(portStr);
		}catch(Exception e){}
		if(port==null){
			log.info("meveo ftp server doesn't start with port {}",portStr);
			return;
		}
		log.debug("start mina ftp server ...");
		FtpServerFactory serverFactory = new FtpServerFactory();

		ListenerFactory factory = new ListenerFactory();

		// set the port of the listener
		factory.setPort(port);

		serverFactory.addListener("default", factory.createListener());
		serverFactory.getFtplets().put("meveoFtplet", meveoDefaultFtplet);
		MeveoFtpUserManagerFactory managerFactory=new MeveoFtpUserManagerFactory(userService);
		UserManager userManager = managerFactory.createUserManager();
		serverFactory.setUserManager(userManager);

		// start the server
		server = serverFactory.createServer();

		server.start();
		log.debug("ftp server is started at port "+portStr);

	}
	
	@PreDestroy
	private void stopServer() {
		try {
			if (server != null) {
				server.stop();
				log.debug("ftp server stoped");
			}
		} catch (Exception e) {
			log.error("Error stoping ftp server", e);
		}
	}
	
	
}
