/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.buaa.act.gephi.plugin.gui;

/**
 *
 * @author song
 */
public enum PluginStatus {
    INIT(0),DB_READY(1),SUBNET_FOUND(2),NET_IMPORTED(3),CHOOSING_START(4),CHOOSING_END(5);
    private int id=0;
    PluginStatus(int sid){
        this.id = sid;
    }
    public int value(){
        return id;
    }
}
