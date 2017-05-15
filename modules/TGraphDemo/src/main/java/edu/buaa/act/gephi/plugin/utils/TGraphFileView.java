package edu.buaa.act.gephi.plugin.utils;


import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.filechooser.FileView;
import org.openide.util.ImageUtilities;

/**
 * Created by song on 16-6-13.
 */
public class TGraphFileView extends FileView {

    String[] requiredFileNames = {"neostore", "neostore.id",
            "neostore.nodestore.db", "neostore.nodestore.db.id",
            "neostore.propertystore.db", "neostore.propertystore.db.id",
            "neostore.propertystore.db.arrays", "neostore.propertystore.db.arrays.id",
            "neostore.propertystore.db.index", "neostore.propertystore.db.index.id",
            "neostore.propertystore.db.index.keys", "neostore.propertystore.db.index.keys.id",
            "neostore.propertystore.db.strings", "neostore.propertystore.db.strings.id",
            "neostore.relationshipstore.db", "neostore.relationshipstore.db.id",
            "neostore.relationshiptypestore.db", "neostore.relationshiptypestore.db.id",
            "neostore.relationshiptypestore.db.names", "neostore.relationshiptypestore.db.names.id",
            "dynNode","dynRelationship"
    };

    Set TGraph_required_files = new HashSet<String>(Arrays.asList(requiredFileNames));

    @Override
    public Icon getIcon(File file) {
        if(accept(file)) {
            return ImageUtilities.loadImageIcon("Neo4j-logo.png", false);
        }else {
            return null;
        }
    }
    
    @Override
    public Boolean isTraversable(File f){
        return !accept(f);
    }

    private boolean accept(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if(files!=null){
                int existingRequiredFiles = 0;
                for (File file : files) {
                    if (TGraph_required_files.contains(file.getName())) {
                        existingRequiredFiles++;
                    }
                }
                return existingRequiredFiles == TGraph_required_files.size();
            }
        }
        return false;
    }
}