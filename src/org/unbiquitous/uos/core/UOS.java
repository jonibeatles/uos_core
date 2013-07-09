package org.unbiquitous.uos.core;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.UUID;

import org.unbiquitous.uos.core.adaptabitilyEngine.AdaptabilityEngine;
import org.unbiquitous.uos.core.adaptabitilyEngine.EventManager;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.adaptabitilyEngine.SmartSpaceGateway;
import org.unbiquitous.uos.core.applicationManager.ApplicationDeployer;
import org.unbiquitous.uos.core.applicationManager.ApplicationManager;
import org.unbiquitous.uos.core.connectivity.ConnectivityManager;
import org.unbiquitous.uos.core.deviceManager.DeviceDao;
import org.unbiquitous.uos.core.deviceManager.DeviceManager;
import org.unbiquitous.uos.core.driverManager.DriverDao;
import org.unbiquitous.uos.core.driverManager.DriverDeployer;
import org.unbiquitous.uos.core.driverManager.DriverManager;
import org.unbiquitous.uos.core.driverManager.DriverManagerException;
import org.unbiquitous.uos.core.driverManager.ReflectionServiceCaller;
import org.unbiquitous.uos.core.messageEngine.MessageEngine;
import org.unbiquitous.uos.core.messageEngine.MessageHandler;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.dataType.UpNetworkInterface;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManagerControlCenter;
import org.unbiquitous.uos.core.network.exceptions.NetworkException;
import org.unbiquitous.uos.core.network.model.NetworkDevice;
import org.unbiquitous.uos.core.network.radar.RadarControlCenter;
import org.unbiquitous.uos.core.ontologyEngine.Ontology;
import org.unbiquitous.uos.core.ontologyEngine.exception.ReasonerNotDefinedException;

/**
 * 
 * This class centralizes the process of initialization e teardown of the
 * middleware and it's dependencies.
 * 
 * @author Fabricio Nogueira Buzeto
 * 
 */
public class UOS {

	private static final String DEVICE_NAME_KEY = "ubiquitos.uos.deviceName";

	private static final Logger logger = Logger
			.getLogger(UOS.class);

	private static String DEFAULT_UBIQUIT_BUNDLE_FILE = "ubiquitos";

	private DriverManager driverManager;
	private ConnectionManagerControlCenter connectionManagerControlCenter;
	private RadarControlCenter radarControlCenter;
	private SecurityManager securityManager;
	private UpDevice currentDevice;

	private ApplicationDeployer applicationDeployer;
	private DeviceManager deviceManager;

	private DriverDao driverDao;
	private DeviceDao deviceDao;
	private ReflectionServiceCaller serviceCaller;
	private EventManager eventManager;
    private Ontology ontology;
    private ResourceBundle resourceBundle;

	private ApplicationManager applicationManager;
        
	
	public static void main(String[] args) throws Exception{
		new UOS().init();
	}
	
	/**
	 * Initializes the components of the uOS middleware using 'ubiquitos' as the
	 * name of the resouce bundle to be used.
	 * 
	 * @throws ContextException
	 */
	public void init() throws ContextException {
		init(DEFAULT_UBIQUIT_BUNDLE_FILE);
	}

	/**
	 * Initializes the components of the uOS middleware acording to the
	 * resourceBundle informed.
	 * 
	 * @param resourceBundleName
	 *            Name of the <code>ResourceBundle</code> to be used for finding
	 *            the properties of the uOS middleware.
	 * @throws ContextException
	 */
	public void init(ResourceBundle resourceBundle) throws ContextException {
		
		try {
			this.resourceBundle = resourceBundle;
			
			// Start Security Manager
			logger.debug("Initializing SecurityManager");
			initSecurityManager();

			/*---------------------------------------------------------------*/
			
			// Start Connection Manager Control Center
			logger.debug("Initializing ConnectionManagerControlCenter");
			initConnectionManagerControlCenter();
			
			logger.debug("Initializing CurrentDevice");
			initCurrentDevice();
			
			/*---------------------------------------------------------------*/
			
			// Start The Message Listener
			logger.debug("Initializing MessageListener");
			initMessageEngine();

			
			/*---------------------------------------------------------------*/
			
			// Start Service Handler
			logger.debug("Initializing ServiceHandler");
			initAdaptabilityEngine();

			/*---------------------------------------------------------------*/
			
			get(MessageEngine.class).setDeviceManager(deviceManager);

			/*---------------------------------------------------------------*/
			
            initOntology();
                        
            //FIXME: This is trash
            get(SmartSpaceGateway.class).init(get(AdaptabilityEngine.class), currentDevice, securityManager,
					get(ConnectivityManager.class),
					deviceManager, driverManager, applicationDeployer, ontology);

			/*---------------------------------------------------------------*/
			
			// Start Connectivity Manager
			logger.debug("Initializing ConnectivityManager");
			initConnectivityManager();

			// Start Radar Control Center
			logger.debug("Initializing RadarControlCenter");
			initRadarControlCenter();

			// Initialize the deployed Drivers
			driverManager.initDrivers(get(SmartSpaceGateway.class));

			// Start The Applications within the middleware
			logger.debug("Initializing Applications");
			initApplications();
		} catch (DriverManagerException e) {
			logger.error(e);
			throw new ContextException(e);
		} catch (NetworkException e) {
			throw new ContextException(e);
		} catch (SecurityException e) {
			throw new ContextException(e);
		}
	}

	/**
	 * Initializes the components of the uOS middleware acording to the
	 * resourceBundle informed.
	 * 
	 * @param resourceBundleName
	 *            Name of the <code>ResourceBundle</code> to be used for finding
	 *            the properties of the uOS middleware.
	 * @throws ContextException
	 */
	public void init(String resourceBundleName) throws ContextException {
		// Log start Message
		logger.info("..::|| Starting uOS ||::..");

		// Get the resource Bundle
		logger.debug("Retrieving Resource Bundle Information");
		ResourceBundle resourceBundle = ResourceBundle
				.getBundle(resourceBundleName);

		init(resourceBundle);
	}

	private void initConnectionManagerControlCenter()
			throws NetworkException {
		connectionManagerControlCenter = null;
		try {
			connectionManagerControlCenter = new ConnectionManagerControlCenter(
					get(MessageEngine.class), resourceBundle);
		} catch (NetworkException ex) {
			logger.error(
					"[Starting] Error creating Connection Manager Control Center.",
					ex);
			throw ex;
		}
	}

	private void initRadarControlCenter()
			throws NetworkException {
		radarControlCenter = new RadarControlCenter(deviceManager,
				resourceBundle, connectionManagerControlCenter);
		radarControlCenter.startRadar();
	}

	private void initMessageEngine() {
		MessageHandler messageHandler = new MessageHandler(resourceBundle, 
												connectionManagerControlCenter,
												securityManager,
												get(ConnectivityManager.class)
											);
		get(MessageEngine.class).init(get(AdaptabilityEngine.class), get(AdaptabilityEngine.class),
				securityManager, connectionManagerControlCenter, 
				messageHandler);
	}

	private void initAdaptabilityEngine() throws DriverManagerException, SecurityException {

		// Start Driver Manager
		logger.debug("Initializing DriverManager");
		driverManager = new DriverManager(currentDevice, getDriverDao(), getDeviceDao(), getServiceCaller());

		// Deploy service-drivers
		DriverDeployer driverDeployer = new DriverDeployer(driverManager,resourceBundle);
		driverDeployer.deployDrivers();
		
		// Init Adaptability Engine
		get(AdaptabilityEngine.class).init(connectionManagerControlCenter, driverManager,
				currentDevice, this, get(MessageEngine.class), 
				get(ConnectivityManager.class), getEventManager());
		
		// Start Device Manager
		logger.debug("Initializing DeviceManager");
		initDeviceManager();

	}

	private void initCurrentDevice() {

		// Collect device informed name
		currentDevice = new UpDevice();
		if (resourceBundle.containsKey(DEVICE_NAME_KEY)){
			currentDevice.setName(resourceBundle.getString(DEVICE_NAME_KEY));
		}else{
			try {
				currentDevice.setName(InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e) {
				new ContextException(e);
			}
		}

		if (currentDevice.getName().equals("localhost")){
			UUID uuid = UUID.randomUUID();
			currentDevice.setName(uuid.toString());
		}
		
		//get metadata
		currentDevice.addProperty("platform",System.getProperty("java.vm.name"));
		
		// Collect network interface information
		List<NetworkDevice> networkDeviceList = connectionManagerControlCenter.getNetworkDevices();
		List<UpNetworkInterface> networks = new ArrayList<UpNetworkInterface>();
		for (NetworkDevice nd : networkDeviceList) {
			UpNetworkInterface nInf = new UpNetworkInterface();
			nInf.setNetType(nd.getNetworkDeviceType());
			nInf.setNetworkAddress(connectionManagerControlCenter.getHost(nd.getNetworkDeviceName()));
			networks.add(nInf);
			logger.info(nd.getNetworkDeviceType() + " > " + nd.getNetworkDeviceName());
		}

		currentDevice.setNetworks(networks);
	}

	private void initSecurityManager() throws SecurityException {
		securityManager = new SecurityManager(resourceBundle);
	}

	private void initDeviceManager() throws SecurityException {
		deviceManager = new DeviceManager(currentDevice, 
								getDeviceDao(),getDriverDao(), 
								getConnectionManagerControlCenter(), 
								get(ConnectivityManager.class), 
								get(SmartSpaceGateway.class), getDriverManager());
	}

	private void initConnectivityManager() {
		//Read proxying attribute from the resource bundle
		boolean doProxying = false;

		try {
			if ((resourceBundle.getString("ubiquitos.connectivity.doProxying")).equalsIgnoreCase("yes")) {
				doProxying = true;
			}
		} catch (MissingResourceException e) {
			logger.info("No proxying attribute found in the properties. Proxying set as false.");
		}

		get(ConnectivityManager.class).init(this, get(SmartSpaceGateway.class), doProxying);
	}

	private void initApplications()throws ContextException {
		applicationManager = new ApplicationManager(resourceBundle, get(SmartSpaceGateway.class));
		applicationDeployer = new ApplicationDeployer(resourceBundle,applicationManager);
		applicationDeployer.deployApplications();
		applicationManager.startApplications();
	}

	private void initOntology() {
		try {
			// TODO: check if this is right
			if (!resourceBundle.containsKey("ubiquitos.ontology.path"))
				return;
			ontology = new Ontology(resourceBundle);
			// ontology.setDriverManager(driverManager);
			ontology.initializeOntology();
		} catch (ReasonerNotDefinedException ex) {
			logger.info(ex);
		}
	}
        
	/**
	 * Shutdown the middleware infrastructure.
	 */
	public void tearDown() {

		// inform the applications about the teardown process
		try {
			applicationManager.tearDown();
		} catch (Exception e) {
			logger.error(e);
		}

		// inform the drivers about the teardown process
		driverManager.tearDown();

		// inform the network layer about the tear down process
		connectionManagerControlCenter.tearDown();

		// stopApplications all radars
		radarControlCenter.stopRadar();

	}

	/**
	 * @return Returns the Driver Manager of this Application Context.
	 */
	public DriverManager getDriverManager() {
		return driverManager;
	}

	/**
	 * @return Returns the ConnectionManagerControlCenter of this Application
	 *         Context.
	 */
	public ConnectionManagerControlCenter getConnectionManagerControlCenter() {
		return connectionManagerControlCenter;
	}

	/**
	 * @return the securityManager
	 */
	public SecurityManager getSecurityManager() {
		return securityManager;
	}

	/**
	 * @return the deviceManager
	 */
	public DeviceManager getDeviceManager() {
		return deviceManager;
	}

	/**
	 * @return The Gateway used by Drivers and Applications to interact with the
	 *         Smart Space
	 */
	public Gateway getGateway() {
		return get(SmartSpaceGateway.class);
	}

	/**
	 * @return The ApplicationDeployer used to deploy applications dynamically
	 *         into the middleware.
	 */
	public ApplicationManager getApplicationManager() {
		return applicationManager;
	}

	/**
	 * @return The DriverDao used to store all driver info
	 *         into the middleware.
	 */
	public DriverDao getDriverDao() {
		if (driverDao == null) driverDao = new DriverDao(resourceBundle);
		return driverDao;
	}
	
	public RadarControlCenter getRadarControlCenter(){
		return radarControlCenter;
	}
	
	/**
	 * @return The DeviceDao used to store all device info
	 *         into the middleware.
	 */
	public DeviceDao getDeviceDao() {
		if (deviceDao == null) deviceDao = new DeviceDao(resourceBundle);
		return deviceDao;
	}
	
	private ReflectionServiceCaller getServiceCaller(){
		if (serviceCaller == null) serviceCaller = new ReflectionServiceCaller(connectionManagerControlCenter);
		return serviceCaller;
	}
	
	private EventManager getEventManager(){
		if (eventManager == null) eventManager = new EventManager(get(MessageEngine.class));
		return eventManager;
	}

	public UpDevice device() {
		return currentDevice;
	}
	
	
//	-----------------------------------------------------------------
	
	private Map<Class, Object> instances = new HashMap<Class, Object>();
	
	private <T> T get(Class<T> clazz){
		if (!instances.containsKey(clazz)){
			try {
				instances.put(clazz, clazz.newInstance());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return (T) instances.get(clazz);
	}
}
