# Excel-Driven Playwright Automation Framework (Java)

A **zero-code automation framework** built with Playwright and Java that enables non-technical users to create and execute automated tests using Excel spreadsheets.

---

## 🚀 Key Features

- ✅ **Zero-Code Automation** - Define tests entirely in Excel (no programming required)
- ✅ **Multi-Test Support** - Functional, Validation, and Regression testing
- ✅ **Parallel Execution** - Run multiple tests concurrently
- ✅ **Multi-Browser** - Chrome, Firefox, Edge, Safari support
- ✅ **Dynamic Data Generation** - Auto-generate random names, emails, phones
- ✅ **Smart Placeholders** - Reuse generated data with `{{USER_1}}`, `{{LAST_NAME}}`
- ✅ **Rich Verifications** - 15+ verification types (textcontains, count, tablerowcontains, etc.)
- ✅ **Multi-Tab Support** - Automatic tab switching and management
- ✅ **Responsive Design** - Auto-adjusts to screen size
- ✅ **Headless/Headed Mode** - Configure via Excel
- ✅ **HTML Reports** - Detailed test execution reports with screenshots
- ✅ **Email Integration** - Auto-send reports via email

---

## 📋 Prerequisites

- **Java 11+**
- **Maven 3.6+**
- **Excel** (for test data)

---

## 🛠️ Setup

### 1. Clone Repository
```bash
git clone <repository-url>

```

### 2. Install Dependencies
```bash
mvn clean install
```

### 3. Configure Environment
Edit `src/main/resources/conf.env`:
```properties
MASTER_EXCEL_PATH=/path/to/MasterDataSheet.xlsx
REPORT_TITLE=Your Project - Test Execution Report

# Email Configuration
EMAIL_TO=recipient@example.com
EMAIL_CC=cc@example.com
EMAIL_FROM=sender@example.com
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password
```

---

## 📊 Excel Structure

### **MasterDataSheet.xlsx**
Controls which modules to execute:

| Status | Module Name | Excel Path | Index File Name |
|--------|-------------|------------|-----------------|
| Active | Student Login | /path/to/StudentLogin.xlsx | Student_Login.html |
| Inactive | Teacher Login | /path/to/TeacherLogin.xlsx | Teacher_Login.html |

### **Module Excel Files**
Each module has these sheets:

#### **1. Config Sheet**
| Test Type | Browser Type | Run mode | No of Iterations | Regression ID | Application URL | Headless Mode |
|-----------|--------------|----------|------------------|---------------|-----------------|---------------|
| Functional | Chrome | Sequential | 1 | - | https://app.com | No |
| Regression | Firefox | Parallel | 3 | reg1,reg2,reg3 | https://app.com | Yes |

#### **2. Login Sheet**
| Field Name | Field Type | Field Xpath | Functional Login | Validation Login | reg1 | reg2 |
|------------|------------|-------------|------------------|------------------|------|------|
| Email | Input | //input[@id='email'] | user@test.com | val@test.com | reg1@test.com | reg2@test.com |
| Password | Input | //input[@id='password'] | 1234 | 1234 | 1234 | 1234 |
| Login Button | Click | //button[@type='submit'] | - | - | - | - |

#### **3. Step Groups Sheet**
| Step ID | Step Group Name |
|---------|-----------------|
| 1 | Login to Application |
| 2 | Create New Student |
| 3 | Verify Student Created |

#### **4. Functional/Regression/Validation Sheet**
| Step ID | Field Name | Field Type | Field Xpath | Field Value | Verification Type | Verification Xpath | Expected Value | Scenario Type |
|---------|------------|------------|-------------|-------------|-------------------|-------------------|----------------|---------------|
| 1 | Student Name | input | //input[@name='name'] | random.name | textcontains | //div[@class='success'] | Success | Positive |
| 2 | Email | input | //input[@name='email'] | random.email | - | - | - | Positive |
| 3 | Submit | click | //button[@type='submit'] | - | visible | //div[@class='alert'] | - | Positive |
| 4 | Verify Name | - | - | - | tablerowcontains | //div[@class='ag-row'] | {{LAST_NAME}} | Positive |

---

## 🎯 Supported Actions

### **Field Types**
- `input` - Fill text fields
- `click` / `button` - Click elements
- `dropdown` - Select single option
- `multiselect` - Select multiple options
- `image` / `video` - Upload files
- `checkbox` - Check/uncheck
- `hover` - Hover over element
- `doubleclick` - Double-click
- `dragdrop` - Drag and drop
- `scroll` - Scroll to element
- `wait` - Wait for milliseconds
- `newtab` - Open new tab
- `switchtab` - Switch to tab by index
- `closetab` - Close current tab
- `refresh` - Reload page
- `backward` / `forward` - Browser navigation
- `switchframe` - Switch to iframe

### **Verification Types**
- `textcontains` - Check if text exists
- `textequals` - Exact text match
- `textcapture` - Capture text for later use
- `textcaptureequals` - Compare with captured text
- `visible` / `notvisible` - Element visibility
- `count` - Count elements
- `countincrease` / `countdecrease` - Compare counts
- `countequals` - Compare with captured count
- `urlcontains` / `urlequals` - URL verification
- `valueequals` / `valuecontains` - Input value verification
- `tablerowcontains` / `tablerownotcontains` - Table row verification

---

## 🔄 Dynamic Data Generation

### **Generate Random Data**
```
Field Value: random.name
Field Value: random.email
Field Value: random.phone
```

### **Reuse Generated Data**
```
Field Value: {{USER_1}}      → First generated name
Field Value: {{EMAIL_1}}     → First generated email
Field Value: {{LAST_NAME}}   → Last generated name
```

---

## ▶️ Run Tests

### **Run All Active Modules**
```bash
mvn test -Dtest=Main
```

### **Run Specific Test**
```bash
java -cp target/classes:target/test-classes com.kriyatec.automation.Main
```

---

## 📈 Reports

Reports are generated in `reports/` directory:

```
reports/
├── index.html                          # Master report (all modules)
├── StudentLogin/
│   ├── Student_Login.html             # Module index
│   ├── FT_StudentLogin_Chrome_01.html # Functional test
│   └── RT_StudentLogin_Chrome_01.html # Regression test
└── TeacherLogin/
    └── ...
```

---

## 📧 Email Reports

Configure email in `conf.env`, then reports are automatically sent after execution with:
- Master report (index.html)
- All module reports
- Individual test reports
- Summary metrics

---

## 🎨 Advanced Features

### **Multi-Tab Testing**
```
Field Type: newtab
Field Value: {{EMAIL_1}}  → Opens new tab and logs in with generated email
```

### **Capture & Verify Count**
```
Step 1: Verification Type: count, Post Step Verification: Yes  → Captures count
Step 2: (Perform action that changes count)
Step 3: Verification Type: countincrease  → Verifies count increased
```

### **Table Row Verification with Scroll**
```
Verification Type: tablerowcontains
Verification Xpath: //div[@class='ag-row']
Expected Value: {{LAST_NAME}}
```
Automatically scrolls table to find the row.

---

## 🐛 Troubleshooting

### **Browser doesn't open**
- Check `Headless Mode` in Excel Config is set to `No`
- Verify browser drivers are installed

### **Element not found**
- Verify XPath is correct
- Check if element is in iframe
- Add wait time before action

### **Email not sending**
- Use Gmail App Password (not regular password)
- Enable "Less secure app access" or use OAuth2

### **Tests fail on different screen sizes**
- Framework auto-adjusts to screen size
- Ensure web app is responsive

---

## 📝 Best Practices

1. **Use meaningful Step IDs** - Group related steps
2. **Capture data before verification** - Use `textcapture` then `textcaptureequals`
3. **Use placeholders** - Reuse generated data with `{{LAST_NAME}}`
4. **Add waits** - Use `wait` field type for dynamic content
5. **Verify after actions** - Always add verification steps
6. **Use Positive/Negative scenarios** - Test both happy and error paths

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

---

## 📄 License

This project is licensed under the MIT License.

---

## 👥 Support

For issues and questions:
- Create an issue in the repository
- Contact: support@example.com

---

## 🎓 Training

Non-technical users can learn to create tests in **30 minutes**:
1. Understand Excel structure
2. Learn basic XPath (or use browser DevTools)
3. Define test steps in Excel
4. Run and review reports

**No coding required!** 🎉


##How to run this code/automation - command to run this

mvn clean compile exec:java -Dexec.mainClass="com.kriyatec.automation.Main" 
