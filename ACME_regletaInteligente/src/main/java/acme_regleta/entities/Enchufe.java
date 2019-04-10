package acme_regleta.entities;

public class Enchufe {

	private int idEnchufe;
	private String aliasEnchufe;
	
	
	public Enchufe(int idEnchufe, String aliasEnchufe) {
		super();
		this.idEnchufe = idEnchufe;
		this.aliasEnchufe = aliasEnchufe;
	}
	
	
	public Enchufe() {
		this(0, "");
	}


	public int getIdEnchufe() {
		return idEnchufe;
	}


	public void setIdEnchufe(int idEnchufe) {
		this.idEnchufe = idEnchufe;
	}


	public String getAliasEnchufe() {
		return aliasEnchufe;
	}


	public void setAliasEnchufe(String aliasEnchufe) {
		this.aliasEnchufe = aliasEnchufe;
	}
	
	
	
	
}
