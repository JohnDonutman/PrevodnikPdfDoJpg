import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import javafx.scene.control.*;



import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Prevodnik {

    private final List<String> vstupniCestySeznam = new ArrayList<>();
    private final List<String> vystupniCestySeznam = new ArrayList<>();
    private final String vstupniCestySeznamCsv = "vstupniCesty.csv";
    private final String vystupniCestySeznamCsv = "vystupniCesty.csv";
    private int pocetStranDokumentu = 0;
    private int pocetSouboruVeSlozce = 0;
    private int aktualniCisloStrankyDokumentu = 0;
    private int aktualniCisloSouboruDokumentu = 0;

    public void prevadejPdfSouborDoJpg(File pdfSoubor, File vystupniSlozka, TextArea logArea) {
        try {
            PDDocument pdfDokument = Loader.loadPDF(pdfSoubor);
            PDFRenderer pdfRenderer = new PDFRenderer(pdfDokument);
            String cestaVystupniSlozkyPdfSouboru = vystupniSlozka.getAbsolutePath() + File.separator + pdfSoubor.getName().replace(".pdf", "");

            // Vytvoření složky s názvem PDF souboru
            File vystupniSlozkaPdfSouboru = new File(cestaVystupniSlozkyPdfSouboru);
            if (!vystupniSlozkaPdfSouboru.exists()) {
                if (vystupniSlozkaPdfSouboru.mkdirs()) {
                    logArea.appendText("Výstupní složka PDF souboru vytvořena: " + vystupniSlozkaPdfSouboru.getAbsolutePath() + "\n");
                } else {
                    logArea.appendText("Chyba při vytváření výstupní složky PDF souboru: " + vystupniSlozkaPdfSouboru.getAbsolutePath() + "\n");
                    return;
                }
            }

            pocetStranDokumentu = pdfDokument.getNumberOfPages();
            aktualniCisloStrankyDokumentu = 0;
            for (int page = 0; page < pocetStranDokumentu; page++) {
                aktualniCisloStrankyDokumentu++;
                prevedStrankuPdfDokumentu(logArea, pdfRenderer, page, vystupniSlozkaPdfSouboru);
            }

            pdfDokument.close();
            logArea.appendText("Převod dokončen.\n");
        } catch (IOException e) {
            logArea.appendText("Chyba při zpracování souboru: " + pdfSoubor.getName() + "\n" + e.getMessage() + "\n");
        }
    }

    private void prevedStrankuPdfDokumentu(TextArea logArea, PDFRenderer pdfRenderer, int page, File vystupniSlozkaPdfSouboru) throws IOException {
        BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
        String nazevSouboruPlusStranka = vystupniSlozkaPdfSouboru.getAbsolutePath() + File.separator + "page-" + (page + 1) + ".jpg";
        ImageIO.write(bufferedImage, "jpg", new File(nazevSouboruPlusStranka));
        logArea.appendText("Uloženo: " + nazevSouboruPlusStranka + "\n");
    }

    public void prevadejSlozkuPdfDoJpg(File vstupniSlozka, File vystupniSlozka, TextArea logArea) {
        if (!vstupniSlozka.isDirectory()) {
            logArea.appendText("Zadaná cesta není složka: " + vstupniSlozka.getAbsolutePath() + "\n");
            return;
        }

        File[] pdfSouboryVeSlozce = vstupniSlozka.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

        if (pdfSouboryVeSlozce == null || pdfSouboryVeSlozce.length == 0) {
            logArea.appendText("Ve složce nebyly nalezeny žádné PDF soubory: " + vstupniSlozka.getAbsolutePath() + "\n");
            return;
        }

        pocetSouboruVeSlozce = pdfSouboryVeSlozce.length;
        aktualniCisloSouboruDokumentu = 0;
        for (File pdfSoubor : pdfSouboryVeSlozce) {
            aktualniCisloSouboruDokumentu++;
            prevadejPdfSouborDoJpg(pdfSoubor, vystupniSlozka, logArea);
        }

        logArea.appendText("Převod všech PDF souborů dokončen.\n");
        ulozCestuDoSeznamu(vstupniSlozka.getAbsolutePath(), vstupniCestySeznam, vstupniCestySeznamCsv);
        ulozCestuDoSeznamu(vystupniSlozka.getAbsolutePath(), vystupniCestySeznam, vystupniCestySeznamCsv);
    }

    public void ulozCestuDoSeznamu(String cesta, List<String> cestySeznam, String csvSoubor) {
        int MAXIMALNI_POCET_CEST = 5;
        if (cestySeznam.size() == MAXIMALNI_POCET_CEST) {
            cestySeznam.remove(0);
            cestySeznam.add(cesta);
        } else if (!cestySeznam.contains(cesta)) {
            cestySeznam.add(cesta);
        }

        try (BufferedWriter zapisovacSouboru = new BufferedWriter(new FileWriter(csvSoubor))) {
            for (String cestaZeSeznamu : cestySeznam) {
                zapisovacSouboru.write(cestaZeSeznamu);
                zapisovacSouboru.newLine();
            }
            ulozSeznamCest(csvSoubor, cestySeznam);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> nactiSeznamCest(String cestySoubor) {
        File souborSCestamiPoslednichSlozek = new File(cestySoubor);
        List<String> seznamCest = new ArrayList<>();

        // Kontrola, zda soubor existuje
        if (!souborSCestamiPoslednichSlozek.exists()) {
            try {
                if (souborSCestamiPoslednichSlozek.createNewFile()) {
                    System.out.println("Soubor " + cestySoubor + " byl vytvořen.");
                }
            } catch (IOException e) {
                System.err.println("Chyba při vytváření souboru: " + e.getMessage());
            }
            return seznamCest; // Vrátí prázdný seznam, protože soubor je nový
        }

        try (BufferedReader cteckaSouboru = new BufferedReader(new FileReader(cestySoubor))) {
            String radek;
            while ((radek = cteckaSouboru.readLine()) != null) {
                seznamCest.add(radek);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return seznamCest;
    }

    private void ulozSeznamCest(String cestySoubor, List<String> cestySeznam) {
        try (BufferedWriter zapisovacSouboru = new BufferedWriter(new FileWriter(cestySoubor))) {
            for (String cesta : cestySeznam) {
                zapisovacSouboru.write(cesta);
                zapisovacSouboru.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getVstupniCestySeznamCsv() {
        return vstupniCestySeznamCsv;
    }

    public String getVystupniCestySeznamCsv() {
        return vystupniCestySeznamCsv;
    }

    public int getPocetStranDokumentu() {
        return pocetStranDokumentu;
    }

    public int getPocetSouboruVeSlozce() {
        return pocetSouboruVeSlozce;
    }

    public int getAktualniCisloStrankyDokumentu() {
        return aktualniCisloStrankyDokumentu;
    }

    public int getAktualniCisloSouboruDokumentu() {
        return aktualniCisloSouboruDokumentu;
    }
}
