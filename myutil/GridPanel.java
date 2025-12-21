package myutil;

import java.awt.*;

public class GridPanel extends Panel
{
	GridBagLayout gbl;
	GridBagConstraints gbc;

	public GridPanel(String font, int fontsize)
	{
		initvars();
		setFont(new Font(font, Font.BOLD, fontsize));
	}

	public GridPanel()
	{
		initvars();
		setFont(new Font("Courier", Font.BOLD, 12));
	}

	void initvars()
	{
		gbl = new GridBagLayout();
		gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;

		setLayout(gbl);
	}

	public void add(Component c, int x, int y, int w, int h)
	{
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = w;
		gbc.gridheight = h;
		gbl.setConstraints(c,gbc);
		add(c);
	}

	public void add(Component c, int x, int y)
	{
		add(c, x, y, 1, 1);
	}
}
