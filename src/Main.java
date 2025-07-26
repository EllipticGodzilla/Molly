import files.FileCipher;
import files.FileInterface;
import files.Logger;
import files.ModLoader;
import gui.*;
import gui.themes.GraphicsSettings;
import gui.settingsFrame.SettingsFrame;
import gui.temppanel.TempPanel;
import gui.temppanel.TempPanel_info;
import network.*;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.*;

public class Main {
    private static byte[] file_key_test;

    //contiene metodi aggiunti dalle mod che vengono eseguiti alla chiusura del software
    private static Vector<Method> end_methods = new Vector<>();

    public static void main() {
        Runtime.getRuntime().addShutdownHook(shut_down);

        //salva i byte da comparare per controllare se la password inserita è corretta
        try {
            file_key_test = Main.class.getClassLoader().getResourceAsStream("files/FileCipherKey.dat").readAllBytes();
        }
        catch (Exception e) { //il file contenente la password è mancante
            System.out.println("impossibile trovare il file contenente la password per decifrare i file all'interno dell'eseguibile\n" + e.getMessage());
            System.exit(0);
        }

        //inizializza tutte le classi
        FileInterface.search_files();
        Vector<Method>[] runnable_method = new ModLoader().load_mods();
        end_methods = runnable_method[3];

        fire_methods(runnable_method[0]);
        Logger.log("eseguiti tutti i metodi da chiamare subito a startup");

        GraphicsSettings.load_from_files();
        MollyFrame.init();
        SettingsFrame.init();
        ServerInterface.init_standards();

        fire_methods(runnable_method[1]);
        Logger.log("eseguiti tutti i metodi da chiamare prima di decifrare i file");

        request_file_psw(runnable_method[2]);
    }

    /*
     * continua a richiedere la password per decifrare i files finché non viene inserita quella corretta
     */
    private static void request_file_psw(Vector<Method> runnable_methods) {
        while (true) { //continua finché non viene raggiunto il return
            //chiede la password per decifrare i file cifrati
            Vector<Object> input = TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_MSG,
                    false,
                    "inserisci la chiave per i database:"
            ).set_psw_indices(0), Thread.currentThread());

            byte[] psw_hash = test_psw((char[]) input.elementAt(0));
            if (psw_hash == null) {
                Logger.log("è stata inserita una password per decifrare i file errata");

                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "password non corretta, riprovare"
                ), null);
            }
            else {
                Logger.log("inserita la password corretta per decifrare i file");

                FileCipher.init_ciphers(psw_hash); //inizializza File_cipher

                //decifra tutte le informazioni contenute dei file cifrati e aggiorna tutte le variabili interne con i dati
                FileInterface.load_from_disk();
                GraphicsSettings.add_file_managers();

                fire_methods(runnable_methods);
                Logger.log("eseguiti tutti i metodi da chiamare dopo aver decifrato i file");

                return;
            }
        }
    }

    private static byte[] test_psw(char[] psw) {
        //ricava un array di byte[] da password[] prendendo il secondo byte per ogni char in esso
        byte[] psw_bytes = new byte[psw.length];
        for (int i = 0; i < psw.length; i++) {
            psw_bytes[i] = (byte) psw[i];
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA3-512");
            byte[] hash = md.digest(psw_bytes);

            //la seconda metà dell hash viene utilizzata per controllare che la password sia corretta, confrontandola con una copia che ha in un file dell hash corretto
            byte[] comp_hash = Arrays.copyOfRange(hash, 32, 64);

            if (Arrays.equals(comp_hash, file_key_test)) {
                return hash;
            }

            return null;
        }
        catch (Exception e) {
            Logger.log("errore nel calcolare l'hash della password", true);
            Logger.log(e.getMessage(), true, '\n', false);

            return null;
        }
    }

    private static final Thread shut_down = new Thread(() -> {
        try {
            Logger.log("inizio la chiusura del programma");

            if (ServerInterface.is_connected()) {
                ServerInterface.close(true);
            }
            Logger.log("scollegato dal server");

            for (Method method : end_methods) {
                method.invoke(null);
            }

            FileInterface.update_files();

            Logger.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    private static void fire_methods(Vector<Method> methods) {
        for (Method method : methods) {
            try {
                method.invoke(null);
            }
            catch (Exception e) {
                Logger.log("impossibile eseguire il metodo: " + method.getName() + " fra quelli da invocare a startup", true);
                Logger.log(e.getMessage(), true);
            }
        }
    }
}