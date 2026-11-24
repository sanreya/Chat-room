import static java.awt.Component.CENTER_ALIGNMENT;
import static java.awt.Component.LEFT_ALIGNMENT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class Client {
	private static final Color CREAM = new Color(247, 243, 235);
	private static final Color PAPER = new Color(255, 253, 248);
	private static final Color SAGE = new Color(54, 76, 64);
	private static final Color SAGE_LIGHT = new Color(225, 234, 221);
	private static final Color TERRACOTTA = new Color(183, 111, 77);
	private static final Color INK = new Color(43, 55, 47);
	private static final Color MUTED = new Color(126, 134, 124);
	private static final Color BORDER = new Color(231, 224, 212);
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

	private final JFrame frame = new JFrame("The Little Room");
	private final JPanel pages = new JPanel(new java.awt.CardLayout());
	private final JTextField nicknameField = new JTextField();
	private final JTextField hostField = new JTextField("localhost");
	private final JLabel loginStatus = new JLabel(" ");
	private final JLabel connectionStatus = new JLabel("A little room for good conversations");
	private final JPanel messageList = new JPanel();
	private final JTextField messageField = new JTextField();
	private final JButton sendButton = new CozyButton("Send");
	private final JButton joinButton = new CozyButton("Join the room");
	private final JLabel userName = label("You", 14, PAPER, Font.BOLD);

	private volatile boolean done;
	private volatile Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private String nickname;

	private Client() {
		buildInterface();
	}

	private void buildInterface() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println("Using default appearance: " + e.getMessage());
		}

		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setMinimumSize(new Dimension(760, 560));
		frame.setSize(1000, 700);
		frame.setLocationRelativeTo(null);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				leaveRoom();
				frame.dispose();
			}
		});

		pages.add(createWelcomePage(), "welcome");
		pages.add(createChatPage(), "chat");
		frame.setContentPane(pages);
		frame.setVisible(true);
		nicknameField.requestFocusInWindow();
	}

	private JPanel createWelcomePage() {
		JPanel background = new JPanel(new GridBagLayout());
		background.setBackground(CREAM);

		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(PAPER);
		card.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDER),
				new EmptyBorder(38, 42, 34, 42)));
		card.setPreferredSize(new Dimension(470, 560));

		JLabel eyebrow = label("A LITTLE CORNER OF THE INTERNET", 11, TERRACOTTA, Font.BOLD);
		eyebrow.setAlignmentX(LEFT_ALIGNMENT);
		card.add(eyebrow);
		card.add(Box.createVerticalStrut(18));

		JLabel title = label("Come on in.", 34, INK, Font.BOLD);
		title.setAlignmentX(LEFT_ALIGNMENT);
		card.add(title);
		card.add(Box.createVerticalStrut(10));

		JLabel subtitle = label("<html>A cozy little room for the people<br>you’re happy to hear from.</html>",
				15, MUTED, Font.PLAIN);
		subtitle.setAlignmentX(LEFT_ALIGNMENT);
		card.add(subtitle);
		card.add(Box.createVerticalStrut(34));

		card.add(fieldLabel("YOUR NAME"));
		card.add(Box.createVerticalStrut(8));
		styleField(nicknameField);
		nicknameField.setToolTipText("The name everyone will see");
		nicknameField.addActionListener(e -> connect());
		card.add(nicknameField);
		card.add(Box.createVerticalStrut(18));

		card.add(fieldLabel("ROOM ADDRESS"));
		card.add(Box.createVerticalStrut(8));
		styleField(hostField);
		hostField.setToolTipText("The computer running the chat server");
		hostField.addActionListener(e -> connect());
		card.add(hostField);
		card.add(Box.createVerticalStrut(22));

		joinButton.setAlignmentX(LEFT_ALIGNMENT);
		joinButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
		joinButton.addActionListener(e -> connect());
		card.add(joinButton);
		card.add(Box.createVerticalStrut(12));

		loginStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
		loginStatus.setForeground(TERRACOTTA);
		loginStatus.setAlignmentX(LEFT_ALIGNMENT);
		card.add(loginStatus);
		card.add(Box.createVerticalGlue());

		JLabel footer = label("Take a breath. You’re right where you need to be.", 12, MUTED, Font.PLAIN);
		footer.setAlignmentX(LEFT_ALIGNMENT);
		card.add(footer);

		background.add(card);
		return background;
	}

	private JPanel createChatPage() {
		JPanel chat = new JPanel(new BorderLayout());
		chat.setBackground(CREAM);
		chat.add(createSidebar(), BorderLayout.WEST);

		JPanel conversation = new JPanel(new BorderLayout());
		conversation.setBackground(CREAM);

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(PAPER);
		header.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
				new EmptyBorder(17, 26, 17, 26)));
		JLabel roomName = label("The cozy corner", 20, INK, Font.BOLD);
		header.add(roomName, BorderLayout.NORTH);
		connectionStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
		connectionStatus.setForeground(MUTED);
		header.add(connectionStatus, BorderLayout.SOUTH);
		conversation.add(header, BorderLayout.NORTH);

		messageList.setLayout(new BoxLayout(messageList, BoxLayout.Y_AXIS));
		messageList.setBackground(CREAM);
		messageList.setBorder(new EmptyBorder(22, 24, 22, 24));
		showWelcomeMessage();

		JScrollPane scroll = new JScrollPane(messageList);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getViewport().setBackground(CREAM);
		scroll.getVerticalScrollBar().setUnitIncrement(18);
		conversation.add(scroll, BorderLayout.CENTER);

		JPanel composer = new JPanel(new BorderLayout(12, 0));
		composer.setBackground(PAPER);
		composer.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
				new EmptyBorder(16, 22, 16, 22)));
		styleField(messageField);
		messageField.setToolTipText("Write a message and press Enter");
		messageField.addActionListener(e -> sendMessage());
		sendButton.setPreferredSize(new Dimension(92, 46));
		sendButton.addActionListener(e -> sendMessage());
		composer.add(messageField, BorderLayout.CENTER);
		composer.add(sendButton, BorderLayout.EAST);
		conversation.add(composer, BorderLayout.SOUTH);
		chat.add(conversation, BorderLayout.CENTER);
		return chat;
	}

	private JPanel createSidebar() {
		JPanel sidebar = new JPanel();
		sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
		sidebar.setBackground(SAGE);
		sidebar.setBorder(new EmptyBorder(27, 21, 22, 21));
		sidebar.setPreferredSize(new Dimension(225, 0));

		JLabel brand = label("the little room", 20, PAPER, Font.BOLD);
		brand.setAlignmentX(LEFT_ALIGNMENT);
		sidebar.add(brand);
		sidebar.add(Box.createVerticalStrut(8));
		JLabel tagline = label("A soft place to land.", 12, new Color(204, 216, 203), Font.PLAIN);
		tagline.setAlignmentX(LEFT_ALIGNMENT);
		sidebar.add(tagline);

		sidebar.add(Box.createVerticalStrut(42));
		JLabel section = label("YOUR SPACE", 10, new Color(190, 207, 190), Font.BOLD);
		section.setAlignmentX(LEFT_ALIGNMENT);
		sidebar.add(section);
		sidebar.add(Box.createVerticalStrut(12));

		JPanel roomRow = new JPanel(new BorderLayout(10, 0));
		roomRow.setOpaque(false);
		roomRow.setBorder(new EmptyBorder(11, 12, 11, 12));
		JLabel roomIcon = label("#", 16, new Color(236, 214, 188), Font.BOLD);
		roomRow.add(roomIcon, BorderLayout.WEST);
		JLabel roomText = label("the cozy corner", 13, PAPER, Font.BOLD);
		roomRow.add(roomText, BorderLayout.CENTER);
		roomRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
		roomRow.setAlignmentX(LEFT_ALIGNMENT);
		sidebar.add(roomRow);

		sidebar.add(Box.createVerticalStrut(28));
		JPanel note = new JPanel();
		note.setLayout(new BoxLayout(note, BoxLayout.Y_AXIS));
		note.setBackground(new Color(67, 91, 75));
		note.setBorder(new EmptyBorder(15, 14, 15, 14));
		note.setAlignmentX(LEFT_ALIGNMENT);
		note.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
		JLabel noteTitle = label("A gentle reminder", 12, new Color(241, 220, 192), Font.BOLD);
		noteTitle.setAlignmentX(LEFT_ALIGNMENT);
		note.add(noteTitle);
		note.add(Box.createVerticalStrut(8));
		JLabel noteBody = label("<html>Good conversations<br>start with a hello.</html>",
				12, PAPER, Font.PLAIN);
		noteBody.setAlignmentX(LEFT_ALIGNMENT);
		note.add(noteBody);
		sidebar.add(note);

		sidebar.add(Box.createVerticalGlue());
		JLabel userLabel = label("HERE AS", 10, new Color(190, 207, 190), Font.BOLD);
		userLabel.setAlignmentX(LEFT_ALIGNMENT);
		sidebar.add(userLabel);
		sidebar.add(Box.createVerticalStrut(8));
		userName.setAlignmentX(LEFT_ALIGNMENT);
		sidebar.add(userName);
		return sidebar;
	}

	private void showWelcomeMessage() {
		JPanel welcome = new JPanel();
		welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
		welcome.setOpaque(false);
		welcome.setAlignmentX(CENTER_ALIGNMENT);
		JLabel title = label("Make yourself at home", 18, INK, Font.BOLD);
		title.setAlignmentX(CENTER_ALIGNMENT);
		JLabel detail = label("Say hello whenever you’re ready.", 13, MUTED, Font.PLAIN);
		detail.setAlignmentX(CENTER_ALIGNMENT);
		welcome.add(title);
		welcome.add(Box.createVerticalStrut(6));
		welcome.add(detail);
		messageList.add(welcome);
	}

	private void connect() {
		String name = nicknameField.getText().trim();
		String host = hostField.getText().trim();
		if (name.isEmpty() || host.isEmpty()) {
			loginStatus.setText("Add your name and the room address to continue.");
			return;
		}

		done = false;
		nickname = name;
		joinButton.setEnabled(false);
		loginStatus.setForeground(MUTED);
		loginStatus.setText("Finding your room…");

		new SwingWorker<Socket, Void>() {
			@Override
			protected Socket doInBackground() throws Exception {
				Socket newSocket = new Socket();
				try {
					newSocket.connect(new InetSocketAddress(host, 50001), 5000);
					newSocket.setSoTimeout(5000);
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(newSocket.getInputStream()));
					PrintWriter writer = new PrintWriter(newSocket.getOutputStream(), true);
					if (reader.readLine() == null) {
						throw new IOException("The server closed the connection before welcoming you.");
					}
					newSocket.setSoTimeout(0);
					writer.println(name);
					in = reader;
					out = writer;
					socket = newSocket;
					return newSocket;
				} catch (Exception e) {
					try {
						newSocket.close();
					} catch (IOException closeError) {
						e.addSuppressed(closeError);
					}
					throw e;
				}
			}

			@Override
			protected void done() {
				try {
					Socket connectedSocket = get();
					connectionStatus.setText("Connected as " + nickname);
					setUserName(nickname);
					messageList.removeAll();
					showWelcomeMessage();
					messageList.revalidate();
					messageList.repaint();
					((java.awt.CardLayout) pages.getLayout()).show(pages, "chat");
					messageField.requestFocusInWindow();
					messageField.setEnabled(true);
					sendButton.setEnabled(true);
					BufferedReader reader = in;
					Thread readerThread = new Thread(
							() -> readMessages(reader, connectedSocket), "chat-reader");
					readerThread.setDaemon(true);
					readerThread.start();
				} catch (Exception e) {
					Throwable cause = e.getCause() == null ? e : e.getCause();
					loginStatus.setForeground(TERRACOTTA);
					loginStatus.setText("Couldn’t join: " + cause.getMessage());
				} finally {
					joinButton.setEnabled(true);
				}
			}
		}.execute();
	}

	private void setUserName(String name) {
		userName.setText(name);
	}

	private void readMessages(BufferedReader reader, Socket connection) {
		try {
			String line;
			while (!done && socket == connection && (line = reader.readLine()) != null) {
				String message = line;
				SwingUtilities.invokeLater(() -> {
					if (!done && socket == connection) {
						showMessage(message);
					}
				});
			}
			if (!done && socket == connection) {
				SwingUtilities.invokeLater(() -> {
					if (!done && socket == connection) {
						connectionLost("The room closed the connection.");
					}
				});
			}
		} catch (IOException e) {
			if (!done && socket == connection) {
				SwingUtilities.invokeLater(() -> {
					if (!done && socket == connection) {
						connectionLost("Connection lost: " + e.getMessage());
					}
				});
			}
		}
	}

	private void showMessage(String message) {
		if (message.startsWith("Successfully changed nickname to ")) {
			nickname = message.substring("Successfully changed nickname to ".length());
			setUserName(nickname);
			connectionStatus.setText("Connected as " + nickname);
			addSystemMessage("You’re now chatting as " + nickname);
			return;
		}

		if (message.contains(" joined the chat!") || message.contains(" left the chat")
				|| message.contains(" renamed themselves to ")) {
			addSystemMessage(message);
			return;
		}

		int separator = message.indexOf(": ");
		if (separator < 0) {
			addSystemMessage(message);
			return;
		}

		String sender = message.substring(0, separator);
		String text = message.substring(separator + 2);
		addChatBubble(sender, text, sender.equals(nickname));
	}

	private void addSystemMessage(String message) {
		JLabel event = label(message, 12, MUTED, Font.PLAIN);
		event.setHorizontalAlignment(SwingConstants.CENTER);
		event.setAlignmentX(CENTER_ALIGNMENT);
		messageList.add(Box.createVerticalStrut(9));
		messageList.add(event);
		messageList.add(Box.createVerticalStrut(9));
		refreshMessages();
	}

	private void addChatBubble(String sender, String text, boolean mine) {
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(5, 0, 5, 0));

		Bubble bubble = new Bubble(mine ? SAGE_LIGHT : PAPER);
		bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
		bubble.setBorder(new EmptyBorder(10, 14, 9, 14));
		bubble.setMaximumSize(new Dimension(520, Integer.MAX_VALUE));

		JPanel meta = new JPanel(new BorderLayout(18, 0));
		meta.setOpaque(false);
		JLabel senderLabel = label(mine ? "You" : sender, 11,
				mine ? SAGE : TERRACOTTA, Font.BOLD);
		JLabel timeLabel = label(LocalTime.now().format(TIME_FORMAT), 10, MUTED, Font.PLAIN);
		meta.add(senderLabel, BorderLayout.WEST);
		meta.add(timeLabel, BorderLayout.EAST);
		bubble.add(meta);
		bubble.add(Box.createVerticalStrut(5));

		JTextArea body = new JTextArea(text);
		body.setEditable(false);
		body.setLineWrap(true);
		body.setWrapStyleWord(true);
		body.setOpaque(false);
		body.setFocusable(false);
		body.setFont(new Font("SansSerif", Font.PLAIN, 14));
		body.setForeground(INK);
		body.setBorder(null);
		int contentWidth = 0;
		for (String line : text.split("\\R", -1)) {
			contentWidth = Math.max(contentWidth, body.getFontMetrics(body.getFont()).stringWidth(line));
		}
		int width = Math.max(1, Math.min(360, contentWidth));
		body.setSize(new Dimension(width, Short.MAX_VALUE));
		body.setPreferredSize(new Dimension(width, body.getPreferredSize().height));
		bubble.add(body);

		row.add(bubble, mine ? BorderLayout.EAST : BorderLayout.WEST);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		messageList.add(row);
		refreshMessages();
	}

	private void refreshMessages() {
		messageList.revalidate();
		messageList.repaint();
		SwingUtilities.invokeLater(() -> {
			JScrollPane scroll = (JScrollPane) messageList.getParent().getParent();
			scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
		});
	}

	private void sendMessage() {
		String message = messageField.getText().trim();
		if (message.isEmpty() || out == null || done) {
			return;
		}
		out.println(message);
		if (out.checkError()) {
			connectionLost("Your message couldn’t be sent.");
			return;
		}
		messageField.setText("");
		if (message.equals("/quit")) {
			leaveRoom();
			((java.awt.CardLayout) pages.getLayout()).show(pages, "welcome");
		}
	}

	private void connectionLost(String message) {
		connectionStatus.setText(message);
		done = true;
		closeConnection();
		messageField.setEnabled(false);
		sendButton.setEnabled(false);
		addSystemMessage(message);
		loginStatus.setForeground(TERRACOTTA);
		loginStatus.setText(message + " You can try joining again.");
		((java.awt.CardLayout) pages.getLayout()).show(pages, "welcome");
	}

	private void leaveRoom() {
		if (out != null && !done) {
			out.println("/quit");
		}
		done = true;
		closeConnection();
	}

	private void closeConnection() {
		Socket currentSocket = socket;
		socket = null;
		if (currentSocket != null) {
			try {
				currentSocket.close();
			} catch (IOException e) {
				System.err.println("Could not close chat connection: " + e.getMessage());
			}
		}
	}

	private static void styleField(JTextField field) {
		field.setFont(new Font("SansSerif", Font.PLAIN, 14));
		field.setForeground(INK);
		field.setBackground(PAPER);
		field.setCaretColor(TERRACOTTA);
		field.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDER),
				new EmptyBorder(11, 13, 11, 13)));
		field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
		field.setPreferredSize(new Dimension(300, 46));
	}

	private static JLabel fieldLabel(String text) {
		JLabel label = label(text, 10, MUTED, Font.BOLD);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static JLabel label(String text, int size, Color color, int style) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("SansSerif", style, size));
		label.setForeground(color);
		return label;
	}

	private static class Bubble extends JPanel {
		private static final long serialVersionUID = 1L;
		private final Color fill;

		Bubble(Color fill) {
			this.fill = fill;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(java.awt.Graphics graphics) {
			java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
			g.setColor(fill);
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
			g.dispose();
			super.paintComponent(graphics);
		}
	}

	private static class CozyButton extends JButton {
		private static final long serialVersionUID = 1L;

		CozyButton(String text) {
			super(text);
			setFont(new Font("SansSerif", Font.BOLD, 14));
			setForeground(PAPER);
			setBackground(TERRACOTTA);
			setFocusPainted(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
			setBorder(new EmptyBorder(12, 18, 12, 18));
			setMargin(new Insets(0, 0, 0, 0));
		}

		@Override
		protected void paintComponent(java.awt.Graphics graphics) {
			java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
			g.setColor(isEnabled() ? getBackground() : MUTED);
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
			g.dispose();
			super.paintComponent(graphics);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(Client::new);
	}
}
