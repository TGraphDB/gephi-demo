package edu.buaa.act.gephi.plugin.utils;

import org.act.tgraph.demo.utils.Hook;
import org.act.tgraph.demo.utils.TransactionWrapper;

import javax.swing.*;
import java.util.Map;

/**
 * to dispatch gui operation to gui thread.
 * Created by song on 16-5-12.
 */
public abstract class GUIHook<T> implements Hook<T> {

    @Override
    public void handler(final T value) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                guiHandler(value);
            }
        });
    }

    public abstract void guiHandler(T value);
}
