package br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages;

import java.util.HashMap;
import java.util.Map;

import br.unb.unbiquitous.ubiquitos.uos.application.UOSMessageContext;


public class ServiceResponse extends Message{
	
	private Map<String,String> responseData;
	
	private UOSMessageContext messageContext;
	
	public ServiceResponse() {
		setType(Message.Type.SERVICE_CALL_RESPONSE);
	}

	public Map<String,String> getResponseData() {
		return responseData;
	}
	
	public String getResponseData(String key) {
		if (responseData != null)
			return responseData.get(key);
		else
			return null;
	}

	public void setResponseData(Map<String,String> responseData) {
		this.responseData = responseData;
	}
	
	public ServiceResponse addParameter(String key, String value){
		if (responseData == null){
			responseData = new HashMap<String, String>();
		}
		responseData.put(key, value);
		return this;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null){
			return false;
		}
		if (!( obj instanceof ServiceResponse)){
			return false;
		}
		ServiceResponse temp = (ServiceResponse) obj; 
		
		if (	!( this.responseData == temp.responseData || (this.responseData != null && this.responseData.equals(temp.responseData)))){
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		if (this.responseData != null){
			hash += this.responseData.hashCode();
		}
			
		if (hash != 0){
			return hash;
		}
		
		return super.hashCode();
	}

	public UOSMessageContext getMessageContext() {
		return messageContext;
	}

	public void setMessageContext(UOSMessageContext messageContext) {
		this.messageContext = messageContext;
	}
}
