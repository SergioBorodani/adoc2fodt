package ru.curs.adoc2fodt;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.asciidoctor.Asciidoctor;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static org.asciidoctor.Asciidoctor.Factory.create;
import static org.asciidoctor.OptionsBuilder.options;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

            int adocDotIndex = adocFile.indexOf(".");
            String adocFilePathWithoutExtension = adocFile.substring(0, adocDotIndex);

            File adocFilePathFile = new File(adocFile).getParentFile();
            String adocFilePath = adocFilePathFile.getAbsolutePath();

            String zipDirectoryPath = adocFilePath + "/zip";

            try {
                new File(zipDirectoryPath).mkdirs();

                Document fodt = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new InputStreamReader(
                                new FileInputStream(new File(adocFilePathWithoutExtension + ".fodt")),
                                "UTF-8")));
                Document content = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new InputStreamReader(
                                new ClassPathResource("/content.xml").getInputStream(), "UTF-8")));
                Document meta = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new InputStreamReader(
                                new ClassPathResource("/meta.xml").getInputStream(), "UTF-8")));
                Document styles = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new InputStreamReader(
                                new ClassPathResource("/styles.xml").getInputStream(), "UTF-8")));

                Node fodtAutomaticStyles = fodt.getElementsByTagName("office:automatic-styles").item(0);
                Node importedFodtAutomaticStyles = content.importNode(fodtAutomaticStyles, true);
                Node contentAutomaticStyles = content.getElementsByTagName("office:automatic-styles").item(0);
                contentAutomaticStyles.getParentNode().replaceChild(importedFodtAutomaticStyles, contentAutomaticStyles);

                Node fodtBody = fodt.getElementsByTagName("office:body").item(0);
                Node importedFodtBody = content.importNode(fodtBody, true);
                Node contentBody = content.getElementsByTagName("office:body").item(0);
                contentBody.getParentNode().replaceChild(importedFodtBody, contentBody);

                Node fodtMeta = fodt.getElementsByTagName("office:meta").item(0);
                Node importedFodtMeta = meta.importNode(fodtMeta, true);
                Node metaMeta = meta.getElementsByTagName("office:meta").item(0);
                metaMeta.getParentNode().replaceChild(importedFodtMeta, metaMeta);

                Node fodtStyles = fodt.getElementsByTagName("office:styles").item(0);
                Node importedFodtStyles = styles.importNode(fodtStyles, true);
                Node stylesStyles = styles.getElementsByTagName("office:styles").item(0);
                stylesStyles.getParentNode().replaceChild(importedFodtStyles, stylesStyles);

                importedFodtAutomaticStyles = styles.importNode(fodtAutomaticStyles, true);
                Node stylesAutomaticStyles = styles.getElementsByTagName("office:automatic-styles").item(0);
                stylesAutomaticStyles.getParentNode().replaceChild(importedFodtAutomaticStyles, stylesAutomaticStyles);

                Node fodtMasterStyles = fodt.getElementsByTagName("office:master-styles").item(0);
                Node importedFodtMasterStyles = styles.importNode(fodtMasterStyles, true);
                Node stylesMasterStyles = styles.getElementsByTagName("office:master-styles").item(0);
                stylesMasterStyles.getParentNode().replaceChild(importedFodtMasterStyles, stylesMasterStyles);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");

                FileWriterWithEncoding writerWithEncoding = new FileWriterWithEncoding(
                        new File(zipDirectoryPath + "/content.xml"), StandardCharsets.UTF_8);
                transformer.transform(new DOMSource(content), new StreamResult(writerWithEncoding));
                writerWithEncoding.close();

                FileWriter writer = new FileWriter(new File(zipDirectoryPath + "/meta.xml"));
                transformer.transform(new DOMSource(meta), new StreamResult(writer));
                writer.close();

                writer = new FileWriter(new File(zipDirectoryPath + "/styles.xml"));
                transformer.transform(new DOMSource(styles), new StreamResult(writer));
                writer.close();

                InputStream src = new ClassPathResource("/manifest.rdf").getInputStream();
                Path dest = Paths.get(zipDirectoryPath + "/manifest.rdf");
                if (dest.toFile().exists()) {
                    FileUtils.forceDelete(dest.toFile());
                }
                FileUtils.copyInputStreamToFile(src, dest.toFile());

                src = new ClassPathResource("/mimetype").getInputStream();
                dest = Paths.get(zipDirectoryPath + "/mimetype");
                if (dest.toFile().exists()) {
                    FileUtils.forceDelete(dest.toFile());
                }
                FileUtils.copyInputStreamToFile(src, dest.toFile());

//                src = new ClassPathResource("/layout-cache").getInputStream();
//                dest = Paths.get(zipDirectoryPath + "/layout-cache");
//                if (dest.toFile().exists()) {
//                    FileUtils.forceDelete(dest.toFile());
//                }
//                FileUtils.copyInputStreamToFile(src, dest.toFile());

                src = new ClassPathResource("/settings.xml").getInputStream();
                dest = Paths.get(zipDirectoryPath + "/settings.xml");
                if (dest.toFile().exists()) {
                    FileUtils.forceDelete(dest.toFile());
                }
                FileUtils.copyInputStreamToFile(src, dest.toFile());

                new File(zipDirectoryPath + "/Configurations2/accelerator").mkdirs();
                new File(zipDirectoryPath + "/Configurations2/floater").mkdirs();
                new File(zipDirectoryPath + "/Configurations2/images.Bitmaps").mkdirs();
                new File(zipDirectoryPath + "/Configurations2/menubar").mkdirs();
                new File(zipDirectoryPath + "/Configurations2/popupmenu").mkdirs();
                new File(zipDirectoryPath + "/Configurations2/progressbar").mkdirs();
                new File(zipDirectoryPath + "/Configurations2/statusbar").mkdirs();
                new File(zipDirectoryPath + "/Configurations2/toolbar").mkdirs();
                new File(zipDirectoryPath + "/Configurations2/toolpanel").mkdirs();
                new File(zipDirectoryPath + "/Thumbnails").mkdirs();
                new File(zipDirectoryPath + "/META-INF").mkdirs();

                src = new ClassPathResource("/META-INF/manifest.xml").getInputStream();
                dest = Paths.get(zipDirectoryPath + "/META-INF/manifest.xml");
                if (dest.toFile().exists()) {
                    FileUtils.forceDelete(dest.toFile());
                }
                FileUtils.copyInputStreamToFile(src, dest.toFile());

//                src = new ClassPathResource("/Configurations2/accelerator/current.xml").getInputStream();
//                dest = Paths.get(zipDirectoryPath + "/Configurations2/accelerator/current.xml");
//                if (dest.toFile().exists()) {
//                    FileUtils.forceDelete(dest.toFile());
//                }
//                FileUtils.copyInputStreamToFile(src, dest.toFile());

                String sourceFile = zipDirectoryPath;
                FileOutputStream fos = new FileOutputStream(adocFilePathWithoutExtension + ".odt");
                ZipOutputStream zipOut = new ZipOutputStream(fos);
                File dirToZip = new File(sourceFile);
                for (File fileToZip : dirToZip.listFiles()) {
                    zipFile(fileToZip, fileToZip.getName(), zipOut);
                }
                zipOut.close();
                fos.close();

                FileUtils.forceDelete(new File(zipDirectoryPath));
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }
}
