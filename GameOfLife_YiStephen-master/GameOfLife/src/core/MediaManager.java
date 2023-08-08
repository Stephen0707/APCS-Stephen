//---------------------------------------------------------------------------------
//DO NOT MODIFY!!!
//---------------------------------------------------------------------------------

package core;

import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JPanel;

public interface MediaManager
{
    void initialize(CustomAppearance appearance, JPanel customPanel);
    void close();
    int getRows();
    int getColumns();
    void drawImage(int row, int column, String imageFilename);
    void eraseImage(int row, int column);
    void drawText(int row, int column, String text, Color color);
    void drawText(int row, int column, String text, Color color, int pointSize);
    void paintSolidColor(int row, int column, Color color);
    void pause(int ms);
    void playNote(int note, int duration);
    void playChord(int[] notes, int duration);
    void setInstrument(int num);
    String getPressedKey();
    int getMouseRow();
    int getMouseColumn();
    boolean isMouseButtonPressed(int button);
    void updateGridAppearance();
    void updateTitle();
    int getWindowLocationX();
    int getWindowLocationY();
    void updateWindowLocationX(int x);
    void updateWindowLocationY(int y);
    void disallowMultipleKeyCalls(String errorMessage);
    void allowMultipleKeyCalls();
}