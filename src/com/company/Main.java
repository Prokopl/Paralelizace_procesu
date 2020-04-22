package com.company;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner; // Import the Scanner class to read text files
import org.apache.commons.lang3.StringUtils;

public class Main {

    public static void main(String[] args) throws IOException {
        // ZAHAJENI MERENI 1 (doba nacteni dat ze souboru + nacteni stopwords)
        Instant start = Instant.now();

        // nacteni stopwords ze souboru a ulozeni do pole stringu "stopwords"
        Path filePath = new File("stopwords2.txt").toPath();
        Charset charset = Charset.defaultCharset();
        List<String> stringList = Files.readAllLines(filePath, charset);
        String[] stopwords = stringList.toArray(new String[]{});

        // vytvoreni stacku pro umisteni recenzi ze souboru
        Stack<String> STACK = new Stack<String>();

        try {
            // nacteni souboru s daty a vytvoreni scanneru pro cteni
            File myObj = new File("Automotive_5.json");
            Scanner myReader = new Scanner(myObj);
            // zpracovani kazdeho radku souboru s daty
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                // vydolovani textu recenze z JSONu cele recenze
                String review = StringUtils.substringBetween(data, "\"reviewText\": \"", "\", \"overall\"");
                // vlozeni recenze na stack s recenzemi
                STACK.push(review);

            }
            // ukonceni cteni ze souboru
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Soubor nebyl nalezen.");
            e.printStackTrace();
        }

        // UKONCENI MERENI 1 (doba nacteni dat ze souboru + nacteni stopwords)
        Instant finish = Instant.now();
        // vypocet doby mezi zahajenim a ukoncemi mereni a vypis na stdout
        long timeElapsed = Duration.between(start, finish).toMillis();
        System.out.println("Doba nacteni dat a stopwords ze souboru: " + timeElapsed + " ms");



        // ZAHAJENI MERENI 2 (algoritmus - proces filtrovani stopwords)
        Instant start2 = Instant.now();

        // vytvoreni listu pro vysledne stringy po zpracovani
        List<String> results = new ArrayList<String>();
        // vytvoreni promenne pro ulozeni jedne zpracovane recenze (aktualni)
        String result = "";
        // regularni vyraz pouzity pro parsovani jednotlivych slov z recenze
        String regex = "([^a-zA-Z']+)'*\\1*";

        // pruchod vsemi recenzemi
        while (!STACK.empty()) {
            // ziskani jedne recenze a ulozeni do promenne
            String review = STACK.pop();
            // rozdeleni recenze na jednotliva slova podle regularniho vyrazu a ulozeni slov do pole
            String[] wordsOfReview = review.split(regex);

            // pruchod kazdym slovem z dane recenze
            for (int i = 0; i < wordsOfReview.length; i++) {
                // promennou "slovo bylo nalezeno ve stopwords" nastavim na false
                boolean foundInStopWords = false;
                // dane slovo zbavim pripadne mezeny a vsechna pismena zmenim na mala
                String word = wordsOfReview[i].toLowerCase().trim();
                // pruchod polem stopwords
                for (int j = 0; j < stopwords.length; j++) {
                    // stopword zbavim pripadne mezeny a vsechna pismena zmenim na mala
                    String stopword = stopwords[j].toLowerCase().trim();
                    // pokud je stopwords shodne s aktualnim slovem z recenze, boolean promennou nastavim na true
                    // a ukoncim prohledavani v seznamu stopwords pro aktualni slovo
                    if (word.equals(stopword)) {
                        foundInStopWords = true;
                        break;
                    }
                }
                // pokud slovo nebylo mezi stopwords nalezeno, pridam jej do retezce vystupu aktualni recenze
                if (foundInStopWords == false) {
                    result += wordsOfReview[i];
                    result += " ";
                }
            }

            // do listu zpracovanych recenzi pridam aktualni recenzi
            results.add(result);
            // promennou pro aktualni recenzi vynuluji
            result = "";
        }

        // UKONCENI MERENI 2 (algoritmus - proces filtrovani stopwords)
        Instant finish2 = Instant.now();

        // vypocet doby mezi zahajenim a ukoncenim mereni a vypis na stdout
        long timeElapsed2 = Duration.between(start2, finish2).toMillis();
        System.out.println("Doba procesu filtrovani stopwords: " + timeElapsed2 + " ms");



        // ZAHAJENI MERENI 3 (zapis vysledku do souboru)
        Instant start3 = Instant.now();

        // vytvoreni a otevreni souboru output.txt
        PrintWriter out = new PrintWriter("output.txt");

        // pruchod pres vsechny zpracovane recenze
        for (int i = 0; i < results.size(); i++) {
            // ziskani recenze
            String singleResult = results.get(i);
            // zapis recenze do souboru
            out.println(singleResult);
        }

        // zavreni souboru a ukonceni zapisu
        out.close();

        // UKONCENI MERENI 3 (zapis vysledku do souboru)
        Instant finish3 = Instant.now();
        // vypocet doby mezi zahajenim a ukoncenim mereni a vypis na stdout
        long timeElapsed3 = Duration.between(start3, finish3).toMillis();
        System.out.println("Doba procesu zapisu do souboru: " + timeElapsed3 + " ms");

    }
}
