package core;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle;

public class WindowOptionsDialogManager
{
	private JDialog dlg;
	
	public WindowOptionsDialogManager(JFrame ownerFrame, CustomAppearance ca)
	{
		JLabel wpxLabel = new JLabel("Window Position X");
		JTextField wpxField = new JTextField("" + ca.getWindowLocationX(), 10);
		wpxField.setEditable(false);
		wpxField.setBackground(null);
		wpxField.setBorder(null);
		JLabel wpyLabel = new JLabel("Window Position Y");
		JTextField wpyField = new JTextField("" + ca.getWindowLocationY(), 10);
		wpyField.setEditable(false);
		wpyField.setBackground(null);
		wpyField.setBorder(null);
		JLabel winTitleLabel = new JLabel("Window Title");
		JTextField winTitleField = new JTextField("" + ca.getWindowTitle(), 10);
		JLabel btxLabel = new JLabel("Border Thickness X");
		JTextField btxField = new JTextField("" + ca.getBorderThicknessX(), 10);
		JLabel btyLabel = new JLabel("Border Thickness Y");
		JTextField btyField = new JTextField("" + ca.getBorderThicknessY(), 10);
		JLabel bcLabel = new JLabel("Border color");
		JButton bcButton = new JButton("     ");
		bcButton.setForeground(ca.getBorderColor());
		bcButton.setBackground(ca.getBorderColor());
		bcButton.addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	Color newColor = JColorChooser.showDialog(
                        dlg,
                        "Choose Border Color",
                        bcButton.getForeground());
            	if (newColor != null)
            	{
            		bcButton.setForeground(newColor);
            		bcButton.setBackground(newColor);
            	}
            }
        });
		JLabel ccLabel = new JLabel("Cell background color");
		JButton ccButton = new JButton("     ");
		ccButton.setForeground(ca.getCellColor());
		ccButton.setBackground(ca.getCellColor());
		ccButton.addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	Color newColor = JColorChooser.showDialog(
                        dlg,
                        "Choose Border Color",
                        ccButton.getForeground());
            	if (newColor != null)
            	{
            		ccButton.setForeground(newColor);
            		ccButton.setBackground(newColor);
            	}
            }
        });
		
		JPanel ctlsPanel = new JPanel();
		GroupLayout layout = new GroupLayout(ctlsPanel);
		ctlsPanel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(
				layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addComponent(wpxLabel)
						.addComponent(wpyLabel)
						.addComponent(winTitleLabel)
						.addComponent(btxLabel)
						.addComponent(btyLabel)
						.addComponent(bcLabel)
						.addComponent(ccLabel)
						)
				.addGroup(layout.createParallelGroup()
						.addComponent(wpxField)
						.addComponent(wpyField)
						.addComponent(winTitleField)
						.addComponent(btxField)
						.addComponent(btyField)
						.addComponent(bcButton)
						.addComponent(ccButton)
						)
				);
		layout.setVerticalGroup(
				layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addComponent(wpxLabel)
						.addComponent(wpxField))
				.addGroup(layout.createParallelGroup()
						.addComponent(wpyLabel)
						.addComponent(wpyField))
				.addGroup(layout.createParallelGroup()
						.addComponent(winTitleLabel)
						.addComponent(winTitleField))
				.addGroup(layout.createParallelGroup()
						.addComponent(btxLabel)
						.addComponent(btxField))
				.addGroup(layout.createParallelGroup()
						.addComponent(btyLabel)
						.addComponent(btyField))
				.addGroup(layout.createParallelGroup()
						.addComponent(bcLabel)
						.addComponent(bcButton))
				.addGroup(layout.createParallelGroup()
						.addComponent(ccLabel)
						.addComponent(ccButton))
				);
		
		JPanel okCancelPanel = new JPanel();
		FlowLayout okCancelLayout = new FlowLayout(FlowLayout.RIGHT);
		okCancelPanel.setLayout(okCancelLayout);
		
		JButton okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	int newBtx = 0;
            	int newBty = 0;
            	try
            	{
            		newBtx = Integer.parseInt(btxField.getText());
            		newBty = Integer.parseInt(btyField.getText());
            	}
            	catch (NumberFormatException e)
            	{
            		JOptionPane.showMessageDialog(dlg, "If I ask for an integer, you need to give me an integer", "Error", JOptionPane.ERROR_MESSAGE);
            		return;
            	}
            	ca.setWindowTitle(winTitleField.getText());
            	ca.setBorderThicknessX(newBtx);
            	ca.setBorderThicknessY(newBty);
            	ca.setBorderColor(bcButton.getForeground());
            	ca.setCellColor(ccButton.getForeground());
                dlg.setVisible(false);
            }
        });
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.setMnemonic(KeyEvent.VK_ESCAPE);
		ActionListener cancelActionListener = 
				new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				dlg.setVisible(false);
			}
		};
		cancelBtn.registerKeyboardAction(
				cancelActionListener, 
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), 
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		cancelBtn.addActionListener(cancelActionListener);
		okCancelPanel.add(okBtn);
		okCancelPanel.add(cancelBtn);
		
		JPanel overallPanel = new JPanel();
		overallPanel.setLayout(new BorderLayout());
		overallPanel.add(ctlsPanel, BorderLayout.CENTER);
		overallPanel.add(okCancelPanel, BorderLayout.PAGE_END);
		
		dlg = new JDialog(ownerFrame, "Window Options", true /* modal */);
		dlg.setContentPane(overallPanel);
		dlg.setLocationRelativeTo(ownerFrame);
		dlg.getRootPane().setDefaultButton(okBtn);
		dlg.pack();
	}
	
	public void show()
	{
		dlg.setVisible(true);
	}
}
