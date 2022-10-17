package pt.ulisboa.tecnico.classes.namingserver;

import pt.ulisboa.tecnico.classes.namingserver.exceptions.DuplicateServerException;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ServiceEntry {

	private String serviceName;

	private ConcurrentLinkedQueue<ServerEntry> serverEntries = new ConcurrentLinkedQueue<>();

	public ServiceEntry(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public ConcurrentLinkedQueue<ServerEntry> getServerEntries() {
		return serverEntries;
	}

	public void setServerEntries(ConcurrentLinkedQueue<ServerEntry> serverEntries) {
		this.serverEntries = serverEntries;
	}

	public void addServerEntry(ServerEntry serverEntry){
		for(ServerEntry server : serverEntries){
			if(server.getHostPort().equals(serverEntry.getHostPort())){
				throw new DuplicateServerException(getServiceName(), serverEntry.getHostPort());
			}
		}
		serverEntries.add(serverEntry);
	}

	public boolean removeServerEntry(String hostPort){
		if(serverEntries.removeIf(serverEntrv->serverEntrv.getHostPort().equals(hostPort))==false) return false;
		return true;
	}
}
