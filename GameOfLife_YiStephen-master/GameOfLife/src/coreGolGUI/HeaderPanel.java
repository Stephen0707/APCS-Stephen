package coreGolGUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.border.TitledBorder;

// Houses the GUI controls in the top stripe of the GameOfLifeFrame
// Intentionally not public--only classes in this package should need to use this
class HeaderPanel extends JPanel
{
	private GameOfLifePanel frame;
	private JComboBox<String> tileTypeCombo;
	private JTextField ageText;
	private JTextField stepsText;
	private JTextField pauseText;
	
	// Contains the GUI controls in the top portion of the window, using
	// the following (rough) organization:
	//
	// Tile type: <ComboBox>   Age: <EditControl> [FILL]
	// Steps: <EditControl>   Pause ms: <EditControl> [EVOLVE]
	public HeaderPanel(GameOfLifePanel frameP)
	{
		super();
		
		frame = frameP;

		// First stripe: set & fill commands (tile type, age, fill button)
		
		JPanel setCmdPanel = new JPanel();
		GroupLayout setCmdLayout = new GroupLayout(setCmdPanel);
		setCmdPanel.setLayout(setCmdLayout);
		setCmdLayout.setAutoCreateGaps(true);
		setCmdLayout.setAutoCreateContainerGaps(true);

		JLabel setAndFillInstructions = new JLabel("Click grid to SET individual tile or button to FILL");
		JLabel tileTypeLabel = new JLabel("Tile type:");
		tileTypeCombo = new JComboBox<String>(new String[] { "constant", "rainbow", "mono", "quad", "immigration"});
		JLabel ageLabel = new JLabel("Age:");
		ageText = new JTextField("1", 10);
		JButton fillButton = new JButton("Fill");
		fillButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				frame.sendFillCommand();
			}
		});
		
		setCmdLayout.setHorizontalGroup(
				setCmdLayout.createSequentialGroup()
				.addGroup(setCmdLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(setAndFillInstructions)
						.addGroup(setCmdLayout.createSequentialGroup()
								.addComponent(tileTypeLabel)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(tileTypeCombo)
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED,      GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(ageLabel)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(ageText)
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED,      GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(fillButton)
								)));
		
		setCmdLayout.setVerticalGroup(
				setCmdLayout.createSequentialGroup()
				.addComponent(setAndFillInstructions)
				.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED,      GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addGroup(setCmdLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(tileTypeLabel)
						.addComponent(tileTypeCombo)
						.addComponent(ageLabel)
						.addComponent(ageText)
						.addComponent(fillButton)
						));
		
		TitledBorder setCmdBorder = BorderFactory.createTitledBorder("set and fill commands");
		setCmdPanel.setBorder(setCmdBorder);

		// Second stripe: evolve command (steps, pause, evolve button)
		
		JPanel evolveCmdPanel = new JPanel();
		GroupLayout evolveCmdLayout = new GroupLayout(evolveCmdPanel);
		evolveCmdPanel.setLayout(evolveCmdLayout);
		evolveCmdLayout.setAutoCreateGaps(true);
		evolveCmdLayout.setAutoCreateContainerGaps(true);
		
		JLabel stepsLabel = new JLabel("Number of steps:");
		stepsText = new JTextField("10", 4);
		JLabel pauseLabel = new JLabel("Pause ms:");
		pauseText = new JTextField("400", 6);
		JButton evolveButton = new JButton("Evolve");
		evolveButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				frame.sendEvolveCommand();
			}
		});

		evolveCmdLayout.setHorizontalGroup(
				evolveCmdLayout.createSequentialGroup()	        
				.addComponent(stepsLabel)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(stepsText)
				.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED,      GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(pauseLabel)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(pauseText)
				.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED,      GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(evolveButton));

		evolveCmdLayout.setVerticalGroup(
				evolveCmdLayout.createSequentialGroup()
				.addGroup(evolveCmdLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
						.addComponent(stepsLabel)
						.addComponent(stepsText)
						.addComponent(pauseLabel)
						.addComponent(pauseText)
						.addComponent(evolveButton))
				);
		
		TitledBorder evolveCmdBorder = BorderFactory.createTitledBorder("evolve command");
		evolveCmdPanel.setBorder(evolveCmdBorder);
		
		// Combine the lines together
		
		GroupLayout controlLayout = new GroupLayout(this);
		setLayout(controlLayout);
		controlLayout.setAutoCreateGaps(true);
		controlLayout.setAutoCreateContainerGaps(true);
		
		controlLayout.setHorizontalGroup(
				controlLayout.createSequentialGroup()
				.addGroup(controlLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
						.addComponent(setCmdPanel)
						.addComponent(evolveCmdPanel))
				);
		
		controlLayout.setVerticalGroup(
				controlLayout.createSequentialGroup()
				.addComponent(setCmdPanel)
				.addComponent(evolveCmdPanel));
	}
	
	public String getTileType()
	{
		return (String) tileTypeCombo.getSelectedItem();
	}
	
	public String getTileAge()
	{
		return ageText.getText();
	}
	
	public String getEvolveSteps()
	{
		return stepsText.getText();
	}
	
	public String getEvolvePause()
	{
		return pauseText.getText();
	}
}