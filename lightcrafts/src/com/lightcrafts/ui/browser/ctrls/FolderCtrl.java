/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.browser.folders.FolderBrowserPane;
import com.lightcrafts.ui.browser.folders.FolderTreeListener;
import com.lightcrafts.utils.directory.DirectoryMonitor;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;
import java.io.File;

public class FolderCtrl extends JPanel {

    private FolderBrowserPane tree;

    public FolderCtrl() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        tree = new FolderBrowserPane();
        NavigationPane buttons = new NavigationPane(tree);
        add(buttons);
        add(tree);
        setBorder(LightZoneSkin.getPaneBorder());
    }

    public File getSelection() {
        return tree.getSelectedFile();
    }

    public boolean goToFolder(File folder) {
        return tree.goToFolder(folder);
    }

    @Deprecated
    public boolean goToPicturesFolder() {
        return tree.goToPicturesFolder();
    }

    public void addSelectionListener(FolderTreeListener listener) {
        tree.addSelectionListener(listener);
    }

    public void removeSelectionListener(FolderTreeListener listener) {
        tree.removeSelectionListener(listener);
    }

    // Exposed only so the tree's bounds can be determined for the purpose of
    // dispatching our custom horizontal-scroll mouse wheel events.
    public JComponent getTree() {
        return tree;
    }

    // Special handling for Mighty Mouse and two-finger trackpad
    // horizontal scroll events
    public void horizontalMouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() >= 2) {
            if (tree.isWheelScrollingEnabled()) {
                JScrollBar bar = tree.getHorizontalScrollBar();
                int dir = e.getWheelRotation() < 0 ? -1 : 1;
                int inc = bar.getUnitIncrement(dir);
                int value = bar.getValue() - e.getWheelRotation() * inc;
                bar.setValue(value);
            }
        }
    }

    public void dispose() {
        tree.dispose();
    }

    public void pauseFolderMonitor() {
        DirectoryMonitor monitor = tree.getDirectoryMonitor();
        monitor.suspend();
    }

    public void resumeFolderMonitor() {
        DirectoryMonitor monitor = tree.getDirectoryMonitor();
        monitor.resume(false);
    }

    public void restorePath(String key) {
        tree.restorePath(key);
    }

    public void savePath(String key) {
        tree.savePath(key);
    }
}
