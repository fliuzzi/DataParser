package com.where.utils;

import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CSXmlScanner
{

    public static void main(String[] args)
    {
        if(args.length != 2){
            System.err.println("USAGE: ARG1: CS enhanced.zip  ARG2: listingID");
            return; 
        }
        
        String listingid = args[1];
        
        try{
            ZipFile zipFile = new ZipFile(new File(args[0]));
            for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) e.nextElement();
               
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(zipFile.getInputStream(entry));
                
                doc.getDocumentElement().normalize();
                
                NodeList listOfListings = doc.getElementsByTagName("id");
                Node listingNode = null;
                
                for(int i = 0; i < listOfListings.getLength();i++)
                {
                    listingNode = listOfListings.item(i);
                    
                    if(listingNode.getNodeType() == Node.ELEMENT_NODE){
                        Element listingElement = (Element)listingNode;
                        
                        if(listingElement.getTextContent().trim().equals(listingid.trim()))
                        {
                            System.out.println("Listing found!");
                            System.out.println("This id is located in .xml file: "+entry.getName());
                            
                            Node nextSibling = listingNode.getNextSibling();
                            
                            while(nextSibling != null)
                            {
                                System.out.println(nextSibling.getNodeName() + ": "+nextSibling.getTextContent());
                                nextSibling = nextSibling.getNextSibling();
                            }
                            
                            return;
                        }
                    }
                }
               
               
               
            }
        }
        
            
        catch(Exception e){
            System.err.println("Error!");
            e.printStackTrace();
        }
    }

}
