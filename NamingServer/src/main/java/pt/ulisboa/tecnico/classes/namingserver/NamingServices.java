package pt.ulisboa.tecnico.classes.namingserver;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.DuplicateServiceException;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.UnknowQualifierException;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.UnknownServerException;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class NamingServices {

	/** <serviceName, service> */
	private  ConcurrentHashMap<String, ServiceEntry> services = new ConcurrentHashMap<>();

	public NamingServices() {}

	public ConcurrentHashMap<String, ServiceEntry> getServices() { return services; }

	public ServiceEntry getService(String serviceName){
		return services.get(serviceName);
	}

	public void setServices(ConcurrentHashMap<String, ServiceEntry> services) { this.services = services; }

	public void addService(ServiceEntry serviceEntry){
    	if (services.containsKey(serviceEntry.getServiceName())){
			throw new DuplicateServiceException(serviceEntry.getServiceName());
		}
		services.put(serviceEntry.getServiceName(), serviceEntry);
	}

	public void removeService(ServiceEntry serviceEntry) {
		services.remove(serviceEntry);
	}

	public void register(String serviceName, String hostPort, List<String> qualifiers) throws UnknowQualifierException {
		if(!qualifiers.stream().allMatch(qualifier -> qualifier.equals("P") || qualifier.equals("S"))){
			throw new UnknowQualifierException();
		}
		ServerEntry serverEntry = new ServerEntry(hostPort, qualifiers);
		getService(serviceName).addServerEntry(serverEntry);
	}

	public void delete(String serviceName, String hostPort) throws UnknownServerException {
		if(getService(serviceName).removeServerEntry(hostPort)==false){
			throw new UnknownServerException(hostPort);
		};
	}

	/**
	 * @return a string representing the elements of this object
	 */
	public String list(){
		String list = "";
		for(ServiceEntry service : services.values()){
			for(ServerEntry server: service.getServerEntries()){
				list += String.format("Server{\n\tService: %s\n\thostPort: %s\n\tQualifiers: %s\n}\n",
						service.getServiceName(),
						server.getHostPort(),
						server.getQualifiers());
			}
		}

		return list;
	}
}