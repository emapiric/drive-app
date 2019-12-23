package main;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import korisnici.Korisnik;

public class Server {

	 public static LinkedList<ServerNit> korisnici = new LinkedList<>();
	 public static LinkedList<Korisnik> registrovaniKorisnici = new LinkedList<>();
	 
	 public static void main(String[] args) {
	      	int port = 9000;
	        ServerSocket serverSoket = null;
	        Socket soketZaKomunikaciju = null;
	       
	        try {
	            serverSoket = new ServerSocket(port);
	            while (true) {
	               
	                System.out.println("Cekam na konekciju...");
	                soketZaKomunikaciju = serverSoket.accept();
	                System.out.println("Doslo je do konekcije!");
	                ServerNit klijent = new ServerNit (soketZaKomunikaciju);
	                korisnici.add(klijent);
	                klijent.start();
	            }
	            
	        } catch (IOException e) {
	            System.out.println("Greska prilikom pokretanja servera!");
	        } 

	}


	
}
