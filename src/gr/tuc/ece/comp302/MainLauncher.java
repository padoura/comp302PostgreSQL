package gr.tuc.ece.comp302;

public class MainLauncher {

	public static void main(String[] args) {
		
		DbApp db = new DbApp();
		int linkEstablished = db.askConnection(); // Initial menu
		
		if (linkEstablished==1)
			db.mainMenu(); // Main menu
		
		System.out.println("Bye!");
	}

}
