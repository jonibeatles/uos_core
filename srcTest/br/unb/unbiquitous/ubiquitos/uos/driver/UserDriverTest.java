package br.unb.unbiquitous.ubiquitos.uos.driver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import br.unb.unbiquitous.json.JSONException;
import br.unb.unbiquitous.json.JSONObject;
import br.unb.unbiquitous.ubiquitos.Logger;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.NotifyException;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.ServiceCallException;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.UosEventListener;
import br.unb.unbiquitous.ubiquitos.uos.context.UOSApplicationContext;
import br.unb.unbiquitous.ubiquitos.uos.driverManager.UosDriver;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.Notify;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall.ServiceType;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

@Ignore //FIXME: This test doesn't seems to make much sense
public class UserDriverTest {

	private static final String EMAIL = "talesap2@gmail.com";
	private static final String NAME = "Tales2";
	private static final String LABEL = NAME + UserDriver.SPECIAL_CHARACTER_SEPARATOR + EMAIL;
	private static final String INSTANCE_ID = "My_user_driver";

	private int MAX_NOT_READY_TRIES = 100;

	private static final Logger logger = Logger.getLogger(UserDriverTest.class);

	private static UOSApplicationContext uosApplicationContext;

	private static Gateway gateway;

	@BeforeClass
	public static void setUp() throws Exception {
		System.out.println("\tSetup test");
		uosApplicationContext = new UOSApplicationContext();
		uosApplicationContext.init("ubiquitos_test");
		// uosApplicationContext.init();
		gateway = uosApplicationContext.getGateway();
	}

	@Test
	public void should_list_my_driver() throws Exception {
		System.out.println("\tList my driver test");
		List<UosDriver> listDrivers = uosApplicationContext.getDriverManager().listDrivers();

		Assert.assertNotNull(listDrivers);
		Assert.assertEquals(1, listDrivers.size());
		Assert.assertEquals(UserDriver.USER_DRIVER, listDrivers.get(0).getDriver().getName());
	}

	@Test
	public void should_return_empty_user_data() throws Exception {
		System.out.println("\tReturn empty user data test");

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(UserDriver.EMAIL_PARAM, EMAIL);

		ServiceResponse response = gateway.callService(gateway.getCurrentDevice(), "retrieveUserInfo", UserDriver.USER_DRIVER, INSTANCE_ID, null, parameters);

		Assert.assertNotNull(response);
		Assert.assertNotNull(response.getResponseData());
		String string_user = response.getResponseData().get(UserDriver.USER_PARAM);
		Assert.assertNull(string_user);
	}

	@Test
	public void should_receive_user_added() throws NotifyException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ServiceCallException, JSONException {
		System.out.println("\tAdd user test");

		DummyEventListener dummyNewEventListener = new DummyEventListener();
		gateway.registerForEvent(dummyNewEventListener, gateway.getCurrentDevice(), UserDriver.USER_DRIVER, UserDriver.NEW_USER_EVENT_KEY);

		createUser(LABEL);

		System.out.println("\t\tSynchronous test...");
		Assert.assertEquals(1, dummyNewEventListener.getLastEventCount());
		Assert.assertEquals(dummyNewEventListener.getLastEvent().getEventKey(), UserDriver.NEW_USER_EVENT_KEY);
		Assert.assertEquals(NAME, dummyNewEventListener.getLastEvent().getParameter(UserDriver.NAME_PARAM));
		Assert.assertEquals(EMAIL, dummyNewEventListener.getLastEvent().getParameter(UserDriver.EMAIL_PARAM));

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(UserDriver.EMAIL_PARAM, EMAIL);
		ServiceResponse response = gateway.callService(gateway.getCurrentDevice(), "retrieveUserInfo", UserDriver.USER_DRIVER, INSTANCE_ID, null, parameters);

		System.out.println("\t\tAsynchronous test...");
		Assert.assertNotNull(response);
		Assert.assertNotNull(response.getResponseData());
		String string_user = response.getResponseData().get(UserDriver.USER_PARAM);
		Assert.assertNotNull(string_user);

		JSONObject jsonObject = new JSONObject(string_user);
		Assert.assertEquals(EMAIL, jsonObject.get(UserDriver.EMAIL_PARAM));
		Assert.assertEquals(NAME, jsonObject.get(UserDriver.NAME_PARAM));

	}

	@Test
	public void should_receive_user_changed() throws NotifyException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ServiceCallException, JSONException {
		System.out.println("\tChange information test");

		DummyEventListener dummyEventListener = new DummyEventListener();
		gateway.registerForEvent(dummyEventListener, gateway.getCurrentDevice(), UserDriver.USER_DRIVER, UserDriver.CHANGE_INFORMATION_TO_USER_KEY);

		updateUser(LABEL, 0.99f, 1f, 2f, 3f);

		System.out.println("\t\tSynchronous test...");
		Assert.assertEquals(1, dummyEventListener.getLastEventCount());
		Assert.assertEquals(dummyEventListener.getLastEvent().getEventKey(), UserDriver.CHANGE_INFORMATION_TO_USER_KEY);
		Assert.assertEquals(NAME, dummyEventListener.getLastEvent().getParameter(UserDriver.NAME_PARAM));
		Assert.assertEquals(EMAIL, dummyEventListener.getLastEvent().getParameter(UserDriver.EMAIL_PARAM));

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(UserDriver.EMAIL_PARAM, EMAIL);
		ServiceResponse response = gateway.callService(gateway.getCurrentDevice(), "retrieveUserInfo", UserDriver.USER_DRIVER, INSTANCE_ID, null, parameters);

		System.out.println("\t\tAsynchronous test...");
		Assert.assertNotNull(response);
		Assert.assertNotNull(response.getResponseData());

		String string_user = response.getResponseData().get(UserDriver.USER_PARAM);
		Assert.assertNotNull(string_user);
		JSONObject jsonObject = new JSONObject(string_user);

		Assert.assertEquals(EMAIL, jsonObject.get(UserDriver.EMAIL_PARAM));
		Assert.assertEquals(NAME, jsonObject.get(UserDriver.NAME_PARAM));
		Assert.assertEquals(0.99f, ((Double) jsonObject.get(UserDriver.CONFIDENCE_PARAM)).floatValue());
		Assert.assertEquals(1.0, jsonObject.getDouble(UserDriver.POSITION_X_PARAM));
		Assert.assertEquals(2.0, jsonObject.getDouble(UserDriver.POSITION_Y_PARAM));
		Assert.assertEquals(3.0, jsonObject.getDouble(UserDriver.POSITION_Z_PARAM));
	}

	@Test
	public void should_receive_user_losted() throws NotifyException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ServiceCallException {
		System.out.println("\tLost user test...");

		DummyEventListener dummyLostEventListener = new DummyEventListener();
		gateway.registerForEvent(dummyLostEventListener, gateway.getCurrentDevice(), UserDriver.USER_DRIVER, UserDriver.LOST_USER_EVENT_KEY);

		removeUser(LABEL);

		System.out.println("\t\tSynchronous test...");
		Assert.assertEquals(1, dummyLostEventListener.getLastEventCount());
		Assert.assertEquals(dummyLostEventListener.getLastEvent().getEventKey(), UserDriver.LOST_USER_EVENT_KEY);
		Assert.assertEquals(NAME, dummyLostEventListener.getLastEvent().getParameter(UserDriver.NAME_PARAM));
		Assert.assertEquals(EMAIL, dummyLostEventListener.getLastEvent().getParameter(UserDriver.EMAIL_PARAM));

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(UserDriver.EMAIL_PARAM, EMAIL);
		ServiceResponse response = gateway.callService(gateway.getCurrentDevice(), "retrieveUserInfo", UserDriver.USER_DRIVER, INSTANCE_ID, null, parameters);

		System.out.println("\t\tAsynchronous test...");
		Assert.assertNotNull(response);
		Assert.assertNotNull(response.getResponseData());
		String string_user = response.getResponseData().get(UserDriver.USER_PARAM);
		Assert.assertNull(string_user);
	}

	@Test
	public void should_save_user_image() throws IOException, ServiceCallException {
		// Buscando bytes da frame de teste
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("sampleImage.bmp");
		int availableInput = inputStream.available();
		System.out.println(availableInput);
		byte[] imageData = new byte[availableInput];
		inputStream.read(imageData);

		// Selecionando canal
		int channel = 1;

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(UserDriver.NAME_PARAM, NAME);
		parameters.put(UserDriver.EMAIL_PARAM, EMAIL);
		parameters.put(UserDriver.INDEX_IMAGE_PARAM, "1");
		parameters.put(UserDriver.LENGTH_IMAGE_PARAM, String.valueOf(imageData.length));

		// request stream for save image
		ServiceCall serviceCall = new ServiceCall();
		serviceCall.setDriver(UserDriver.USER_DRIVER);
		serviceCall.setService("saveUserImage");
		serviceCall.setInstanceId(INSTANCE_ID);
		serviceCall.setServiceType(ServiceType.STREAM);
		serviceCall.setChannels(channel);
		serviceCall.setParameters(parameters);

		ServiceResponse response = gateway.callService(gateway.getCurrentDevice(), serviceCall);

		// write image data
		DataOutputStream out = response.getMessageContext().getDataOutputStream(channel);
		out.write(imageData);
		out.close();

		logger.debug("Size => " + out.size());
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		// wait response
		DataInputStream in = response.getMessageContext().getDataInputStream(channel);

		for (int trie = 0; trie < MAX_NOT_READY_TRIES; trie++) {
			int available = in.available();
			if (available > 0) {
				byte[] byteArray = new byte[available];
				in.read(byteArray);
				String s = new String(byteArray);

				logger.debug("CHANNEL[" + channel + "]: RECEBIDO MSG: [" + s + "]");
				break;
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

	}

	@Test
	public void should_remove_user_image() throws IOException, ServiceCallException {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(UserDriver.NAME_PARAM, NAME);
		parameters.put(UserDriver.EMAIL_PARAM, EMAIL);

		ServiceResponse response = gateway.callService(gateway.getCurrentDevice(), "removeUserImages", UserDriver.USER_DRIVER, INSTANCE_ID, null, parameters);

		Assert.assertNull(response.getError());

		response = gateway.callService(gateway.getCurrentDevice(), "listKnownUsers", UserDriver.USER_DRIVER, INSTANCE_ID, null, parameters);

		String returnData = response.getResponseData(UserDriver.RETURN_PARAM);
		Assert.assertNotNull(returnData);

		System.out.println(returnData);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		 uosApplicationContext.tearDown();
	}

	/**
	 * Simulates the creation of user from userDriver
	 * 
	 * @param user
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void createUser(String user) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method method = UserDriverImpl.class.getDeclaredMethod("registerNewUserEvent", String.class, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);
		method.setAccessible(true);
		method.invoke(UserDriverImpl.getInstance(), user, 0.97f, 1.0f, 1.0f, 1.0f);
	}

	/**
	 * Simulates the removal of user from userDriver
	 * 
	 * @param user
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void removeUser(String user) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method method = UserDriverImpl.class.getDeclaredMethod("registerLostUserEvent", String.class);
		method.setAccessible(true);
		method.invoke(UserDriverImpl.getInstance(), user);
	}

	/**
	 * Simulates users update from userDriver
	 * 
	 * @param user
	 * @param confidence
	 * @param x
	 * @param y
	 * @param z
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void updateUser(String user, float confidence, float x, float y, float z) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method method = UserDriverImpl.class.getDeclaredMethod("registerRecheckUserEvent", String.class, String.class, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);
		method.setAccessible(true);
		method.invoke(UserDriverImpl.getInstance(), user, null, confidence, x, y, z);
	}

}

class DummyEventListener implements UosEventListener {

	private Notify lastEvent;

	private int lasteventCount = 0;

	@Override
	public void handleEvent(Notify event) {
		// stores the last recieved event
		System.out.println("\t Receive event in listener for event '" + event.getEventKey() + "' and name '" + event.getParameter(UserDriver.NAME_PARAM) + "'");
		lastEvent = event;
		lasteventCount++;
	}

	/**
	 * @return the lastEvent
	 */
	public Notify getLastEvent() {
		return lastEvent;
	}

	/**
	 * @return the lasteventCount
	 */
	public int getLastEventCount() {
		return lasteventCount;
	}

}
