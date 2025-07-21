package edu.buaa.tgraphdb.utils;

import org.act.tgraph.demo.utils.Hook;

import javax.swing.*;

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
