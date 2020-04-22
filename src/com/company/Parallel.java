package com.company;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.cli.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.*;


public class Parallel {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        // ZPRACOVANI ARGUMENTU
        Options options = new Options();

        Option threadsOpt = new Option("t", "threads", true, "number of threads");
        threadsOpt.setRequired(true);
        options.addOption(threadsOpt);

        Option fileOpt = new Option("f", "file", true, "json file with reviews data");
        fileOpt.setRequired(true);
        options.addOption(fileOpt);

        Option stopwordsOpt = new Option("s", "stopwords", true, "list of stopwords");
        stopwordsOpt.setRequired(true);
        options.addOption(stopwordsOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        // zvoleny pocet vlaken
        int numberOfThreads = Integer.parseInt(cmd.getOptionValue("threads"));

        // zvolena cesta k datovemu souboru
        String inputFilePath = cmd.getOptionValue("file");

        // zvolena cesta k souboru stopwords
        String inputStopwordsPath = cmd.getOptionValue("stopwords");


        System.out.println("Zvoleny pocet vlaken: " + numberOfThreads + "\n*****");

        // ZAHAJENI MERENI 1 (doba nacteni dat ze souboru + nacteni stopwords)
        Instant start = Instant.now();

        // nacteni stopwords ze souboru a ulozeni do pole stringu "stopwords"
        Path filePath = new File(inputStopwordsPath).toPath();
        Charset charset = Charset.defaultCharset();
        List<String> stringList = Files.readAllLines(filePath, charset);
        String[] stopwords = stringList.toArray(new String[]{});

        // vytvoreni stacku pro umisteni recenzi ze souboru
        Stack<String> STACK = new Stack<String>();

        try {
            // nacteni souboru s daty a vytvoreni scanneru pro cteni
            File myObj = new File(inputFilePath);
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

        // vytvoreni poolu s urcitym poctem vlaken
        ForkJoinPool customThreadPool = new ForkJoinPool(numberOfThreads);

        // odeslani ulohy na pool a ziskani vysledku do promenne results
        List<String> results = customThreadPool.submit(
                () -> STACK.parallelStream().map(rev -> filtering(rev, stopwords)).collect(Collectors.toList())
        ).get();

        // ukonceni poolu
        customThreadPool.shutdown();



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

    public static String filtering(String review, String[] stopwords) {
        // regularni vyraz pouzity pro parsovani jednotlivych slov z recenze
        String regex = "([^a-zA-Z']+)'*\\1*";
        // vytvoreni promenne pro ulozeni jedne zpracovane recenze (aktualni)
        String result = "";

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

        return result;
    }

}
