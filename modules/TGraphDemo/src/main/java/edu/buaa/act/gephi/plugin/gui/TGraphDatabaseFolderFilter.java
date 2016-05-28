package edu.buaa.act.gephi.plugin.gui;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * Created by song on 16-5-12.
 */
public class TGraphDatabaseFolderFilter extends FileFilter {
    private static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

    //Accept all directories and all txt files
    public boolean accept(File f) {
        if (f.isDirectory()) {
//            String extension = getExtension(f);
//            if (extension != null) {
//                if (extension.equals("txt")) {
//                    return true;
//                } else {
//                    return false;
//                }
//            }
            return true;
        }else{
            return false;
        }
    }

    //The description of this filter
    public String getDescription() {
        return "TGraph DB folder";
    }

}
