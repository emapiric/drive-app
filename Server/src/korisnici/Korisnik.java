package korisnici;
import java.io.Serializable;
import java.util.LinkedList;

public class Korisnik implements Serializable{

	public String username;
	public String password;
	boolean premium;
	public LinkedList<String> sharedDisks;
	boolean sharedWithPublic;

	public Korisnik(String username, String password, boolean premium) {
		super();
		this.username = username;
		this.password = password;
		this.premium = premium;
		this.sharedDisks = new LinkedList<String>();
		sharedDisks.add(username);
		sharedWithPublic = false;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isPremium() {
		return premium;
	}

	public void setPremium(boolean premium) {
		this.premium = premium;
	}

	public LinkedList<String> getSharedDisks() {
		return sharedDisks;
	}

	public void setSharedDisks(LinkedList<String> sharedDisks) {
		this.sharedDisks = sharedDisks;
	}
	
	public boolean isSharedWithPublic() {
		return sharedWithPublic;
	}

	public void setSharedWithPublic(boolean sharedWithPublic) {
		this.sharedWithPublic = sharedWithPublic;
	}


}
