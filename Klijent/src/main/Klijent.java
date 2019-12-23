package main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Klijent implements Runnable {

    static Socket soketZaKomunikaciju = null;
    static BufferedReader serverInput = null;
    static PrintStream serverOutput = null;
    static BufferedReader unosSaTastature = null;

    public static void main(String[] args) {
        try {
            inicijalizujTokove();
            new Thread(new Klijent()).start();
            while (true) {
            	 String input = serverInput.readLine();
                    System.out.println(input);
                if (input.startsWith(">>> Dovidjenja")) {
                    break;
                }
            }
            soketZaKomunikaciju.close();
        } catch (UnknownHostException e) {
            System.out.println("UNKNOWN HOST!");
        } catch (IOException e) {
            System.out.println("SERVER IS DOWN!!!");
        }
    }
	public static void inicijalizujTokove() throws IOException {
        soketZaKomunikaciju = new Socket("localhost", 9000);
        unosSaTastature = new BufferedReader(new InputStreamReader(System.in));
        serverInput = new BufferedReader(new InputStreamReader(soketZaKomunikaciju.getInputStream()));
        serverOutput = new PrintStream(soketZaKomunikaciju.getOutputStream());
    }

    @Override
    public void run() {
        try {
            while (true) {
                String poruka = unosSaTastature.readLine();
                if (poruka.contains("UPLOAD:")) {
                    String putanjaFajla = poruka.substring(7);
                    upload(putanjaFajla);
                } 
                else {
                    serverOutput.println(poruka);
                }
                if (poruka.contains("QUIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

	public void upload(String putanjaFajla) throws IOException {
        byte[] buffer = new byte[4096];
        FileInputStream fis = new FileInputStream(putanjaFajla);
        OutputStream os = soketZaKomunikaciju.getOutputStream();
        fis.read(buffer, 0, buffer.length);
        os.write(buffer, 0, buffer.length);
        fis.close();
    }
	
	}
