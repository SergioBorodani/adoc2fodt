package ru.curs.adoc2fodt;

import org.asciidoctor.Asciidoctor;

import static org.asciidoctor.Asciidoctor.Factory.create;
import static org.asciidoctor.OptionsBuilder.options;

import java.util.Map;
import java.io.File;

public class Converter {

        public static void main(String[] args) {

            String adocFile = "";
            if (args.length == 0) {
                throw new RuntimeException("Файл adoc не задан");
            } else {
                adocFile = args[0];
            }

            String packtPath = "slim/packt";
            if (args.length == 2) {
                packtPath = args[1];
            }

            Asciidoctor asciidoctor = create();

            Map<String, Object> options = options()
                    .inPlace(true)
                    .templateDirs(
                            new File(packtPath))
                    .backend("packt")
                    .asMap();

            asciidoctor.convertFile(
                    new File(adocFile),
                    options);
        }
}
