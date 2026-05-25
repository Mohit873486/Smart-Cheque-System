package com.chequeprint.preview;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChequePrintPreviewApp extends JFrame {
  private final ChequePreviewPanel previewPanel = new ChequePreviewPanel();
  private final BankTemplateStore templateStore = new BankTemplateStore();
  private final ChequePrintService printService = new ChequePrintService();

  private final JTextField payeeField = new JTextField(26);
  private final JTextField amountNumberField = new JTextField(16);
  private final JTextField amountWordsField = new JTextField(40);
  private final JTextField dateField = new JTextField(14);

  private final JComboBox<BankTemplate> bankCombo = new JComboBox<>();
  private final JComboBox<ChequeField> fieldCombo = new JComboBox<>(ChequeField.values());

  private final JLabel positionLabel = new JLabel();
  private final JLabel canvasLabel = new JLabel();
  private final JLabel statusLabel = new JLabel("Ready");

  private Map<String, BankTemplate> templates = new LinkedHashMap<>();

  public ChequePrintPreviewApp() {
    super("Cheque Printing System - Preview & Multi-Bank Templates");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(1360, 820));
    setLocationRelativeTo(null);
    setLayout(new BorderLayout(12, 12));

    add(buildLeftPanel(), BorderLayout.WEST);
    add(buildPreviewPane(), BorderLayout.CENTER);

    loadTemplatesFromStore();
    bindListeners();
    applyDefaultFormValues();
    updatePreviewData();
  }

  private JPanel buildLeftPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setPreferredSize(new Dimension(420, 800));
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xCBD5E1)),
        BorderFactory.createEmptyBorder(18, 16, 18, 16)));

    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(7, 4, 7, 4);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.weightx = 1;
    c.gridx = 0;
    c.gridy = 0;

    JLabel title = new JLabel("Cheque Print Controls");
    title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
    panel.add(title, c);

    c.gridy++;
    panel.add(sectionLabel("Bank Template"), c);

    c.gridy++;
    panel.add(bankCombo, c);

    c.gridy++;
    panel.add(sectionLabel("Cheque Data"), c);

    c.gridy++;
    panel.add(formRow("Payee Name", payeeField), c);

    c.gridy++;
    panel.add(formRow("Amount (Number)", amountNumberField), c);

    c.gridy++;
    panel.add(formRow("Amount (Words)", amountWordsField), c);

    c.gridy++;
    panel.add(formRow("Date (dd/MM/yyyy)", dateField), c);

    c.gridy++;
    panel.add(new JSeparator(), c);

    c.gridy++;
    panel.add(sectionLabel("Alignment (1 px)") , c);

    c.gridy++;
    panel.add(fieldCombo, c);

    c.gridy++;
    panel.add(buildAlignmentButtons(), c);

    c.gridy++;
    panel.add(positionLabel, c);

    c.gridy++;
    panel.add(canvasLabel, c);

    c.gridy++;
    panel.add(new JSeparator(), c);

    c.gridy++;
    panel.add(buildActionButtons(), c);

    c.gridy++;
    c.weighty = 1;
    panel.add(Box.createVerticalGlue(), c);

    c.gridy++;
    c.weighty = 0;
    statusLabel.setForeground(new Color(0x334155));
    panel.add(statusLabel, c);

    return panel;
  }

  private JScrollPane buildPreviewPane() {
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
    wrapper.setBackground(new Color(0xE2E8F0));

    JLabel label = new JLabel("WYSIWYG Preview (Same renderer used for printing)");
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    label.setForeground(new Color(0x1E293B));
    wrapper.add(label, BorderLayout.NORTH);

    previewPanel.setBorder(BorderFactory.createLineBorder(new Color(0x1E293B), 1));
    wrapper.add(previewPanel, BorderLayout.CENTER);

    JScrollPane scrollPane = new JScrollPane(wrapper);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    return scrollPane;
  }

  private JPanel buildAlignmentButtons() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

    JButton xMinus = new JButton("X -");
    JButton xPlus = new JButton("X +");
    JButton yMinus = new JButton("Y -");
    JButton yPlus = new JButton("Y +");

    xMinus.addActionListener(e -> adjustSelectedField(-1, 0));
    xPlus.addActionListener(e -> adjustSelectedField(1, 0));
    yMinus.addActionListener(e -> adjustSelectedField(0, -1));
    yPlus.addActionListener(e -> adjustSelectedField(0, 1));

    row.add(xMinus);
    row.add(xPlus);
    row.add(yMinus);
    row.add(yPlus);
    return row;
  }

  private JPanel buildActionButtons() {
    JPanel actions = new JPanel();
    actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));

    JButton save = new JButton("Save Templates");
    JButton reload = new JButton("Load Templates");
    JButton resetBank = new JButton("Reset Selected Bank");
    JButton testPrint = new JButton("Test Print");

    save.addActionListener(e -> onSaveTemplates());
    reload.addActionListener(e -> onReloadTemplates());
    resetBank.addActionListener(e -> onResetBankTemplate());
    testPrint.addActionListener(e -> onTestPrint());

    actions.add(save);
    actions.add(Box.createVerticalStrut(8));
    actions.add(reload);
    actions.add(Box.createVerticalStrut(8));
    actions.add(resetBank);
    actions.add(Box.createVerticalStrut(8));
    actions.add(testPrint);

    return actions;
  }

  private JPanel formRow(String labelText, JTextField field) {
    JPanel row = new JPanel(new BorderLayout(8, 0));
    JLabel label = new JLabel(labelText);
    label.setPreferredSize(new Dimension(140, 26));
    row.add(label, BorderLayout.WEST);
    row.add(field, BorderLayout.CENTER);
    return row;
  }

  private JLabel sectionLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
    label.setForeground(new Color(0x0F172A));
    return label;
  }

  private void bindListeners() {
    bankCombo.addActionListener(e -> onBankChanged());
    fieldCombo.addActionListener(e -> updatePositionLabel());

    DocumentListener listener = new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        updatePreviewData();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        updatePreviewData();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        updatePreviewData();
      }
    };

    payeeField.getDocument().addDocumentListener(listener);
    amountNumberField.getDocument().addDocumentListener(listener);
    amountWordsField.getDocument().addDocumentListener(listener);
    dateField.getDocument().addDocumentListener(listener);
  }

  private void applyDefaultFormValues() {
    payeeField.setText("M/s Global Supplies Pvt Ltd");
    amountNumberField.setText("12500.75");
    amountWordsField.setText("Twelve thousand five hundred rupees and seventy five paise only");
    dateField.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
  }

  private void loadTemplatesFromStore() {
    templates = templateStore.loadTemplates();
    DefaultComboBoxModel<BankTemplate> model = new DefaultComboBoxModel<>();
    for (BankTemplate template : templates.values()) {
      model.addElement(template);
    }
    bankCombo.setModel(model);
    if (model.getSize() > 0) {
      bankCombo.setSelectedIndex(0);
      onBankChanged();
    }
  }

  private void onBankChanged() {
    BankTemplate selected = getSelectedTemplate();
    if (selected == null) {
      return;
    }

    previewPanel.setBankTemplate(selected);
    updatePositionLabel();
    statusLabel.setText("Selected bank: " + selected.getDisplayName());
  }

  private void updatePreviewData() {
    LocalDate date = parseDate(dateField.getText().trim());
    String amountText = amountNumberField.getText().trim();

    String words = amountWordsField.getText().trim();
    if (words.isBlank() && !amountText.isBlank()) {
      words = AmountToWords.convert(amountText);
    }

    ChequeData data = new ChequeData(
        payeeField.getText().trim(),
        amountText,
        words,
        date);

    previewPanel.setChequeData(data);
  }

  private LocalDate parseDate(String text) {
    if (text == null || text.isBlank()) {
      return LocalDate.now();
    }

    List<DateTimeFormatter> formatters = new ArrayList<>();
    formatters.add(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    formatters.add(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    formatters.add(DateTimeFormatter.ofPattern("dd MMM yyyy"));

    for (DateTimeFormatter formatter : formatters) {
      try {
        return LocalDate.parse(text, formatter);
      } catch (Exception ignored) {
      }
    }

    statusLabel.setText("Invalid date format. Using today.");
    return LocalDate.now();
  }

  private void adjustSelectedField(int dx, int dy) {
    BankTemplate template = getSelectedTemplate();
    ChequeField field = (ChequeField) fieldCombo.getSelectedItem();
    if (template == null || field == null) {
      return;
    }

    template.adjustPosition(field, dx, dy);
    previewPanel.repaint();
    updatePositionLabel();
    statusLabel.setText("Adjusted " + field.getDisplayName() + " by X=" + dx + " Y=" + dy);
  }

  private void updatePositionLabel() {
    BankTemplate template = getSelectedTemplate();
    ChequeField field = (ChequeField) fieldCombo.getSelectedItem();
    if (template == null || field == null) {
      positionLabel.setText("Position: n/a");
      return;
    }

    FieldPosition pos = template.getPosition(field);
    positionLabel.setText(String.format("%s position: X=%d, Y=%d", field.getDisplayName(), pos.getX(), pos.getY()));
    canvasLabel.setText(String.format("Canvas: %d x %d @ %d DPI (%.2f x %.2f in)",
        ChequeLayout.CANVAS_WIDTH,
        ChequeLayout.CANVAS_HEIGHT,
        ChequeLayout.LOGICAL_DPI,
        ChequeLayout.WIDTH_INCH,
        ChequeLayout.HEIGHT_INCH));
  }

  private void onSaveTemplates() {
    try {
      templateStore.saveTemplates(templates);
      statusLabel.setText("Templates saved successfully.");
      JOptionPane.showMessageDialog(this, "Bank templates saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this,
          "Could not save templates: " + ex.getMessage(),
          "Save Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void onReloadTemplates() {
    loadTemplatesFromStore();
    updatePreviewData();
    statusLabel.setText("Templates loaded from storage.");
  }

  private void onResetBankTemplate() {
    BankTemplate selected = getSelectedTemplate();
    if (selected == null) {
      return;
    }

    Map<String, BankTemplate> defaults = BankTemplateStore.defaultTemplates();
    BankTemplate defaultsForBank = defaults.get(selected.getKey());
    if (defaultsForBank == null) {
      return;
    }

    templates.put(selected.getKey(), defaultsForBank.copy());
    BankTemplate replacement = templates.get(selected.getKey());

    int index = bankCombo.getSelectedIndex();
    bankCombo.insertItemAt(replacement, index);
    bankCombo.removeItemAt(index + 1);
    bankCombo.setSelectedIndex(index);

    previewPanel.setBankTemplate(replacement);
    updatePositionLabel();
    statusLabel.setText("Reset template for " + replacement.getDisplayName());
  }

  private void onTestPrint() {
    updatePreviewData();
    printService.testPrint(previewPanel);
  }

  private BankTemplate getSelectedTemplate() {
    Object obj = bankCombo.getSelectedItem();
    return obj instanceof BankTemplate ? (BankTemplate) obj : null;
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      ChequePrintPreviewApp app = new ChequePrintPreviewApp();
      app.setVisible(true);
    });
  }

  private static final class AmountToWords {
    private static final String[] SMALL = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private AmountToWords() {
    }

    static String convert(String text) {
      try {
        BigDecimal amount = new BigDecimal(text).setScale(2, RoundingMode.HALF_UP);
        long rupees = amount.longValue();
        int paise = amount.remainder(BigDecimal.ONE).movePointRight(2).intValue();

        StringBuilder out = new StringBuilder();
        out.append(numberToWords(rupees)).append(" rupees");
        if (paise > 0) {
          out.append(" and ").append(numberToWords(paise)).append(" paise");
        }
        out.append(" only");
        return out.toString();
      } catch (Exception ex) {
        return text;
      }
    }

    private static String numberToWords(long value) {
      if (value == 0) {
        return "Zero";
      }

      if (value < 0) {
        return "Minus " + numberToWords(-value);
      }

      StringBuilder sb = new StringBuilder();
      if (value / 1_000_000_000 > 0) {
        sb.append(numberToWords(value / 1_000_000_000)).append(" Billion ");
        value %= 1_000_000_000;
      }
      if (value / 1_000_000 > 0) {
        sb.append(numberToWords(value / 1_000_000)).append(" Million ");
        value %= 1_000_000;
      }
      if (value / 1_000 > 0) {
        sb.append(numberToWords(value / 1_000)).append(" Thousand ");
        value %= 1_000;
      }
      if (value / 100 > 0) {
        sb.append(numberToWords(value / 100)).append(" Hundred ");
        value %= 100;
      }
      if (value > 0) {
        if (sb.length() > 0) {
          sb.append("and ");
        }
        if (value < 20) {
          sb.append(SMALL[(int) value]);
        } else {
          sb.append(TENS[(int) (value / 10)]);
          if (value % 10 > 0) {
            sb.append(" ").append(SMALL[(int) (value % 10)]);
          }
        }
      }

      return sb.toString().trim();
    }
  }
}
