package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Base64;
import korisnici.Korisnik;

public class ServerNit extends Thread {

	BufferedReader clientInput = null;
	PrintStream clientOutput = null;
	Socket soketZaKomunikaciju = null;
	Korisnik korisnik = null;
	boolean premium;
	File folder;

	public ServerNit(Socket soket) {
		soketZaKomunikaciju = soket;
	}

	private void inicijalizujTokove() throws IOException {
		clientInput = new BufferedReader(new InputStreamReader(soketZaKomunikaciju.getInputStream()));
		clientOutput = new PrintStream(soketZaKomunikaciju.getOutputStream());
	}

	@Override
	public void run() {

		try {
			deserijalizacija();
			inicijalizujTokove();
			boolean ulogovan = false;
			do {
				clientOutput.println(
						"Da biste se registrovali unesite REGISTRACIJA.\nZa prijavljivanje unesite PRIJAVA.\nZa pregled javnih fajlova unesite PREGLED.\nZa download unesite DOWNLOAD");
				String odgovor = clientInput.readLine();
				switch (odgovor) {
				case "REGISTRACIJA":
					Korisnik k = kreirajKorisnikaPriRegistraciji();
					if (k != null) {
						registrujKorisnika(k);
					}
					break;
				case "PRIJAVA":
					k = kreirajKorisnikaPriPrijavljivanju();
					if (k != null) {
						korisnik = k;
						ulogovan = true;
						clientOutput.println("Uspesno ste se prijavili.\nZa izlazak unesite QUIT");
					}
					break;
				case "PREGLED":
					viewPublic();
					break;
				case "DOWNLOAD":
					downloadPublic(); 
					break;
				default:
					clientOutput.println("Niste uneli validan broj");
					break;
				}
			} while (!ulogovan);

			folder = new File("C:\\Users\\EMA\\eclipse-workspace\\Server\\Drive\\" + korisnik.getUsername());
			viewFiles(folder);
			if (korisnik.isPremium()) {
				meniPremium();
			} else {
				meniObican();
			}

			clientOutput.println(">>> Dovidjenja " + korisnik.getUsername());
			Server.korisnici.remove(this);
			soketZaKomunikaciju.close();
			serijalizacija();

		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			Server.korisnici.remove(this);
			System.out.println(e.getMessage());
		}

	}

	//CUVA LISTU REGISTROVANIH KORISNIKA U FAJL
	private static void serijalizacija() {
		try (FileOutputStream fOut = new FileOutputStream("registrovaniKorisnici.out");
				BufferedOutputStream bOut = new BufferedOutputStream(fOut);
				ObjectOutputStream out = new ObjectOutputStream(bOut)) {
			for (Korisnik k : Server.registrovaniKorisnici) {
				out.writeObject(k);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}
	//UCITAVA REGISTROVANE KORISNIKE IZ FAJLA U LISTU
	private static void deserijalizacija() {
		try (FileInputStream fIn = new FileInputStream("registrovaniKorisnici.out");
				BufferedInputStream bIn = new BufferedInputStream(fIn);
				ObjectInputStream in = new ObjectInputStream(bIn)) {

			Server.registrovaniKorisnici.clear();
			try {
				while (true) {
					Korisnik k = (Korisnik) (in.readObject());
					Server.registrovaniKorisnici.add(k);
				}
			} catch (EOFException e) {
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	//KLIJENT UNOSI PODATKE I KREIRA SE OBJEKAT KORISNIKA KOJI CE SE REGISTROVATI
	public Korisnik kreirajKorisnikaPriRegistraciji() throws IOException {
		clientOutput.println("Unesite username: ");
		String username = clientInput.readLine();
		clientOutput.println("Unesite password: ");
		String password = clientInput.readLine();
		clientOutput.println(
				"Ako zelite da postanete premium korisnik unesite PREMIUM. Da postanete obican korisnik pritisnite bilo sta");
		String odgovor = clientInput.readLine();
		boolean isPremium = shouldSetPremium(odgovor);
		if (postojiOvajKorisnik(username)) {
			clientOutput.println("Username " + username + " je zauzet. Probajte nesto drugo. ");
			return null;
		}
		Korisnik korisnik = new Korisnik(username, password, isPremium);
		return korisnik;
	}

	//PROVERAVA DA LI POSTOJI KORISNIK S PROSLEDJENIM USERNAME-OM
	public boolean postojiOvajKorisnik(String username) {
		for (Korisnik k : Server.registrovaniKorisnici) {
			if (k.username.equals(username)) {
				return true;
			}
		}
		return false;
	}

	//PROVERAVA DA LI KORISNIK ZELI DA BUDE PREMIUM
	public boolean shouldSetPremium(String odgovor) throws IOException {
		if (odgovor.equals("PREMIUM"))
			return true;
		return false;
	}
	
	//UBACUJE KORISIKA U LISTU REGISTROVANIH
	public void registrujKorisnika(Korisnik korisnik) {
		Server.registrovaniKorisnici.add(korisnik);
		kreirajFolder(korisnik.getUsername());
		clientOutput.println("Uspesno ste se registrovali. ");
	}

	//KREIRA FOLDER SA PROSLEDJENOM PUTANJOM
	public void kreirajFolder(String putanja) {
		folder = new File("C:\\Users\\EMA\\eclipse-workspace\\Server\\Drive\\" + putanja);
		folder.mkdir();
	}

	//KREIRA OBJEKAT KORISNIKA NA OSNOVU UNESENIH PODATAKA
	//KREIRANI OBJEKAT CE POSTATI POLJE KLASE UKOLIKO JE != NULL
	//TO CE ZNACITI DA SE KORISNIK USPESNO PRIJAVIO
	public Korisnik kreirajKorisnikaPriPrijavljivanju() throws IOException {
		clientOutput.println("Unesite username: ");
		String username = clientInput.readLine();
		clientOutput.println("Unesite password: ");
		String password = clientInput.readLine();

		Korisnik korisnik = dohvatiKorisnika(username, password);
		return korisnik;
	}

	//VRACA KORISNIKA IZ LISTE REGISTROVANIH S DATIM USERNAME-OM I PASSWORDOM
	public Korisnik dohvatiKorisnika(String username, String password) {
		for (Korisnik k : Server.registrovaniKorisnici) {
			if (k.username.equals(username) && k.password.equals(password)) {
				return k;
			}
		}
		clientOutput.println("Username/password ne postoje. Probajte opet.");
		return null;
	}

	//VRACA KORISNIKA IZ LISTE REGISTROVANIH S DATIM USERNAME-OM
	public Korisnik dohvatiKorisnika(String username) {
		for (Korisnik k : Server.registrovaniKorisnici) {
			if (k.username.equals(username)) {
				return k;
			}
		}
		clientOutput.println("Username/password ne postoje. Probajte opet.");
		return null;
	}
	
	//PROLAZI KROZ LISTU REGISTROVANIH KORISNIKA
	//I ISPISUJE FAJLOVE ONIH KOJI SU SHARE-OVALI SVOJ DISK ZA JAVNOST
//	private void viewPublic() {
//		for (Korisnik k : Server.registrovaniKorisnici) {
//			if (k.isSharedWithPublic()) {
//				clientOutput.println(k.getUsername() + ": ");
//				File folder = new File("C:\\Users\\EMA\\eclipse-workspace\\Server\\Drive\\" + k.getUsername());
//				viewFiles(folder);
//			}
//		}
//
//	}
	private void viewPublic() throws IOException {
		clientOutput.println("Unesi username ciji folder zelis da pogledas");
		String username = clientInput.readLine();
		Korisnik k = dohvatiKorisnika(username);
		if (k.isSharedWithPublic()) {
			File folder = new File("Drive\\"+username);
			viewFiles(folder);
		}
		else {
			clientOutput.println("Taj link nije serovan za javnost");
		}
		

	}
	
	private void viewFiles() {
		// TODO Auto-generated method stub
		
	}

	//NEPRIJAVLJENI KORISNIK DOWNLOADUJE FAJL
	private void downloadPublic() throws IOException {
		clientOutput.println("Unesi username\\imeFajla\nPrimer: korisnik1\\fajl.txt");
		String folderIFajl = clientInput.readLine();
		String prosledjenFolder = folderIFajl.substring(0, folderIFajl.indexOf("\\"));
		if (postojiFajl(folderIFajl) && isPublicDisk(prosledjenFolder)) {
			posaljiDatotekuKlijentu(folderIFajl);
		} else {
			clientOutput.println("Fajl " + folderIFajl + "ne postoji ili nemate pristup njemu.");
		}

	}
	//PROVERAVA DA LI JE PROSLEDJEN DISK PODELJEN ZA JAVNOST
	private boolean isPublicDisk(String username) {
		Korisnik k = dohvatiKorisnika(username);
		return k.isSharedWithPublic();
	}
	//MENI KOJI SE PRIKAZUJE OBICNOM KORISNIKU
	public void meniObican() throws IOException, FileNotFoundException {
		while (true) {
			clientOutput.println(
					"0 - POGLEDAJ SVOJE FAJLOVE\n1 - UPLOAD\n2 - DOWNLOAD\n3 - SHARE WITH USER\n4 - SHARE WITH PUBLIC\n5 - POGLEDAJ DISKOVE PODELJENE SA MNOM\nZa izlaz unesite QUIT");
			String odgovor = clientInput.readLine();

			switch (odgovor) {
			case "0":
				viewFiles(folder);
				break;
			case "1":
				upload();
				break;
			case "2":
				download(); 
				break;
			case "3":
				shareWithUser();
				break;
			case "4":
				shareWithPublic();
				break;
			case "5":
				viewShared();
				break;
			case "QUIT":
				return;
			default:
				clientOutput.println("Pogresan unos");
				break;
			}

		}
	}
	//MENI KOJI SE PRIKAZUJE PREMIUM KORISNIKU
	public void meniPremium() throws IOException, FileNotFoundException {
		while (true) {
			clientOutput.println(
					"0 - POGLEDAJ SVOJE FAJLOVE\n1 - UPLOAD\n2 - DOWNLOAD\n3 - SHARE WITH USER\n4 - SHARE WITH PUBLIC\n5 - POGLEDAJ DISKOVE PODELJENE SA MNOM\n6 - CREATE FOLDER\n7 - RENAME FOLDER\n8 - MOVE FILE\n9 - DELETE FOLDER\nZa izlaz unesite QUIT");
			String odgovor = clientInput.readLine();

			switch (odgovor) {
			case "0":
				viewFiles(folder);
				break;
			case "1":
				upload();
				break;
			case "2":
				download(); 
				break;
			case "3":
				shareWithUser();
				break;
			case "4":
				shareWithPublic();
				break;
			case "5":
				viewShared();
				break;
			case "6":
				createFolder();
				break;
			case "7":
				renameFolder();
				break;
			case "8":
				renameFolder();
				break;
			case "9":
				deleteFolder();
				break;
			case "QUIT":
				return;
			default:
				clientOutput.println("Pogresan unos");
				break;
			}

		}
	}

	
	//LISTA SVE FAJLOVE UNUTAR PROSLEDJENOG FOLDERA
	public void viewFiles(File folder) {

		File[] fajlovi = folder.listFiles();
		if (fajlovi.length > 0) {
			for (File fajl : fajlovi) {
				clientOutput.println(fajl.toString());
				if (fajl.isDirectory()) {
					viewFiles(fajl);
				}
			}
		}
	}

	//CUVA SADRZAJ IZ BUFFERA U FAJL
	public void upload() throws FileNotFoundException, IOException {
		if (!imaMestaNaDrajvu())
			clientOutput.println("Uploadovali ste maksimalan broj datoteka");
		else {
			clientOutput.println("Unesite UPLOAD: pa apsolutnu putanju fajla");
			byte[] sadrzajFajla = ucitajOdKlijentaUBuffer();

			clientOutput.println("Pod kojim imenom hoces da sacuvas fajl?");
			String imeFajla = clientInput.readLine();
			FileOutputStream fos = new FileOutputStream(
					"C:\\Users\\EMA\\eclipse-workspace\\Server\\Drive\\" + korisnik.getUsername() + "\\" + imeFajla);
			fos.write(sadrzajFajla, 0, sadrzajFajla.length);
			fos.close();
		}
	}
	
	//PROVERAVA DA LI JE KORISNIK UPLOADOVAO MAKSIMALAN BROJ FAJLOVA
	private boolean imaMestaNaDrajvu() {
		if (!korisnik.isPremium() && (int) folder.list().length >= 5)
			return false;
		return true;
	}

	//UCITAVA SADRZAJ OD KLIJENTA U BUFFER
	private byte[] ucitajOdKlijentaUBuffer() throws FileNotFoundException, IOException {
		byte[] buffer = new byte[4096];
		InputStream is = soketZaKomunikaciju.getInputStream();
		is.read(buffer, 0, buffer.length);
		return buffer;
	}

	//PITA KOJI FAJL KLIJENT TRAZI I SALJE GA AKO POSTOJI
	private void download() throws IOException {
		clientOutput.println("Unesi putanju npr. username\\podfolder\\imeFajla\nPrimer: korisnik1\\folder1\\fajl.txt");
		String putanja = clientInput.readLine();
		if (validnaPutanja(putanja)) {
			String prosledjenFolder = putanja.substring(0, putanja.indexOf("\\"));
			if (dozvoljenPristupDisku(prosledjenFolder) && postojiFajl(putanja)) {
				posaljiDatotekuKlijentu(putanja);
			} else {
				clientOutput.println("Fajl " + putanja + "ne postoji ili nemate pristup njemu.");
			}
		} else {
			clientOutput.println("Niste lepo uneli putanju");
		}

	}
	//PROVERAVA DA LI JE KLIJENT UNEO VALIDNU PUTANJU
	private boolean validnaPutanja(String putanja) {
		if (putanja.contains("\\"))
			return true;
		return false;
	}
	//PROVERAVA DA LI KORISNIK IMA PRISTUP PROSLEDJENOM DISKU
	public boolean dozvoljenPristupDisku(String prosledjenFolder) {
		return korisnik.getSharedDisks().contains(prosledjenFolder);
	}
	//PROVERAVA DA LI FAJL S PROSLEDJENOM PUTANJOM POSTOJI U FOLDERU DRIVE
	public boolean postojiFajl(String folderIFajl) {
		File fajl = new File("Drive\\" + folderIFajl);
		return fajl.exists();
	}
	//UCITAVA IZ DATOTEKE U BUFFER I SALJE TOK KLIJENTU
	private void posaljiDatotekuKlijentu(String folderIFajl) throws IOException {
		byte[] buffer = new byte[4096];
		String putanja = "Drive\\" + folderIFajl;
		FileInputStream fis = new FileInputStream(putanja);
		OutputStream os = soketZaKomunikaciju.getOutputStream();
		fis.read(buffer, 0, buffer.length);
		if (isBinary(putanja)) {
			Base64.Encoder encoder = Base64.getEncoder();
			buffer = encoder.encode(buffer);
		}
		os.write(buffer, 0, buffer.length);
		fis.close();
	}
	//PROVERAVA DA LI JE DATOTEKA BINARNA
	private boolean isBinary(String putanja) {
		if (putanja.endsWith(".txt"))
			return false;
		return true;
	}
	//TRAZI DISK I KORISNIKA KOME SE SERUJE
	//DODAJE TAJ DISK U LISTU DISKOVA KOJIMA TAJ KORISNIK IMA PRISTUP                                    
	private void shareWithUser() throws IOException {
		clientOutput.println("UNESI IME DISKA KOJI ZELIS DA SHARE-UJES");
		String disk = clientInput.readLine();
		clientOutput.println("UNESI IME KORISNIKA KOME SHARE-UJES");
		String imeKorisnika = clientInput.readLine();
		if (dozvoljenPristupDisku(disk) && postojiOvajKorisnik(imeKorisnika)) {
			Korisnik k = dohvatiKorisnika(imeKorisnika);
			k.sharedDisks.add(disk);
		} else {
			clientOutput.println("Unesen korisnik ne postoji ili nemate pristup unesenom disku.");
		}

	}
	//SERUJE DISK KLIJENTA ZA JAVNOST
	private void shareWithPublic() {
		korisnik.setSharedWithPublic(true);
		clientOutput.println("Vas disk je sada podeljen za javnost");
	}
	//LISTA SADRZAJ SVIH DISKOVA SEROVANIH S KORISNIKOM
	private void viewShared() {
		for (String disk : korisnik.getSharedDisks()) {
			clientOutput.println(disk + ": ");
			File folder = new File("C:\\Users\\EMA\\eclipse-workspace\\Server\\Drive\\" + disk);
			viewFiles(folder);
		}
	}
	//KREIRA FOLDER NA DATOJ PUTANJI
	private void createFolder() throws IOException {
		clientOutput.println(
				"UNESI PUTANJU FOLDERA. DA BUDE PODFOLDER NEKOG FOLDERA UNETI U FORMATU ime_foldera\\ime_podfoldera");
		String imeNovogFoldera = clientInput.readLine();
		File noviFolder = new File(
				"C:\\Users\\EMA\\eclipse-workspace\\Server\\Drive\\" + korisnik.getUsername() + "\\" + imeNovogFoldera);
		noviFolder.mkdir();

	}
	//MENJA IME FOLDERA (PREMESTA GA)
	private void renameFolder() throws IOException {
		clientOutput.println(
				"UNESI PUTANJU FOLDERA ILI DATOTEKE. AKO JE PODFOLDER NEKOG FOLDERA UNETI U FORMATU ime_foldera\\ime_podfoldera");
		String staroIme = clientInput.readLine();
		clientOutput.println(
				"UNESI NOVO IME (PUTANJU) FOLDERA ILI DATOTEKE. DA BUDE PODFOLDER NEKOG FOLDERA UNETI U FORMATU ime_foldera\\ime_podfoldera");
		String novoIme = clientInput.readLine();
		File stari = new File("Drive\\" + korisnik.getUsername() + "\\" + staroIme);
		File novi = new File("Drive\\" + korisnik.getUsername() + "\\" + novoIme);
		if (stari.exists()) {
			stari.renameTo(novi);
		} else {
			clientOutput.println("Folder " + staroIme + "ne postoji.");
		}

	}
	//BRISE FOLDER
	private void deleteFolder() throws IOException {
		clientOutput.println(
				"UNESI PUTANJU FOLDERA KOJI ZELIS DA OBRISES. AKO JE PODFOLDER NEKOG FOLDERA UNETI U FORMATU ime_foldera\\ime_podfoldera");
		String nazivFoldera = clientInput.readLine();
		File folder = new File("Drive\\" + korisnik.getUsername() + "\\" + nazivFoldera);
		boolean uspesnoBrisanje = folder.delete();
		if (!uspesnoBrisanje)
			clientOutput.println("Folder " + folder + " ne postoji ili nije prazan");
	}

}
