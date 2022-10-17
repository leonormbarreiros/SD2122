package pt.ulisboa.tecnico.classes.namingserver;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerEntry {

	private String hostPort;

	private ConcurrentLinkedQueue<String> qualifiers = new ConcurrentLinkedQueue<>();

	public ServerEntry(String hostPort, List<String> qualifiers) {
		this.hostPort = hostPort;
		this.qualifiers.addAll(qualifiers);
	}

	public String getHostPort() {
		return hostPort;
	}

	public void setHostPort(String hostPort) {
		this.hostPort = hostPort;
	}

	public ConcurrentLinkedQueue<String> getQualifiers() {
		return qualifiers;
	}

	public void setQualifiers(ConcurrentLinkedQueue<String> qualifiers) {
		this.qualifiers = qualifiers;
	}

	public void addQualifiers(String qualifier){
		qualifiers.add(qualifier);
	}
}