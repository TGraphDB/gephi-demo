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

    Set<String> neo4jRequireFileNames = new HashSet<String>(Arrays.asList("data", "logs"
//            "neostore.nodestore.db", "neostore.nodestore.db.id",
//            "neostore.propertystore.db", "neostore.propertystore.db.id",
//            "neostore.propertystore.db.arrays", "neostore.propertystore.db.arrays.id",
//            "neostore.propertystore.db.index", "neostore.propertystore.db.index.id",
//            "neostore.propertystore.db.index.keys", "neostore.propertystore.db.index.keys.id",
//            "neostore.propertystore.db.strings", "neostore.propertystore.db.strings.id",
//            "neostore.relationshipstore.db", "neostore.relationshipstore.db.id",
//            "neostore.relationshiptypestore.db", "neostore.relationshiptypestore.db.id",
//            "neostore.relationshiptypestore.db.names", "neostore.relationshiptypestore.db.names.id"
    ));

    Set<String> tGraphDirNames = new HashSet<String>(Arrays.asList("temporal.node.properties","temporal.relationship.properties"));

    boolean isOldTGraph=true;

    @Override
    public Icon getIcon(File file) {
        if(isTGraphDir(file)) {
            if(isOldTGraph) {
                return ImageUtilities.loadImageIcon("Neo4j-logo.png", false);
            }else{
                return ImageUtilities.loadImageIcon("time-machine1.png", false);
            }
        }else {
            return null;
        }
    }
    
    @Override
    public Boolean isTraversable(File f){
        return true;
    }

    private boolean isTGraphDir(File directory) {
        if (directory.getName().toLowerCase().endsWith("db") && directory.isDirectory() && !directory.isHidden() && !directory.getName().startsWith(".")) {
            File[] files = directory.listFiles();
            if(files!=null && files.length==2){
                int existingRequiredFiles = 0;
//                int newCount = 0;
                for (File file : files) {
                    if(file.isDirectory() && ("data".equals(file.getName()) || "logs".equals(file.getName()))){
                        existingRequiredFiles++;
                    }
//                    if (neo4jRequireFileNames.contains(file.getName())) {
//
//                    }else if (tGraphDirNames.contains(file.getName())){
//                        newCount++;
//                        isOldTGraph=false;
//                    }
                }
                return existingRequiredFiles == 2;
            }
        }
        return false;
    }
}