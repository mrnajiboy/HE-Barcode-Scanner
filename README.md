# 📱 HE Barcode Scanner

**A powerful, enterprise-grade barcode scanner for Android with advanced inventory management, webhook integration, and rapid scanning workflows.**

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/mrnajiboy/he-barcode-scanner)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/license-MIT-orange.svg)](LICENSE)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Installation](#-installation)
- [Quick Start](#-quick-start)
- [Scanning Modes](#-scanning-modes)
- [Default Presets](#-default-presets)
- [Item Types](#-item-types)
- [Configuration](#-configuration)
- [Usage Guide](#-usage-guide)
- [Data Formats](#-data-formats)
- [API Integration](#-api-integration)
- [Export & Sharing](#-export--sharing)
- [Technical Details](#-technical-details)
- [FAQ](#-faq)
- [Roadmap](#-roadmap)
- [Support](#-support)

---

## 🎯 Overview

HE Barcode Scanner is a professional-grade Android application built for businesses that need flexible, powerful barcode scanning with real-time API integration. Whether you're managing inventory, tracking shipments, or building custom workflows, HE Barcode Scanner adapts to your needs.

### Why Choose HE Barcode Scanner?

✅ **Enterprise Features, Consumer Simplicity**
- Professional webhook integration
- But simple enough for small businesses

✅ **Truly Customizable**
- Create unlimited item types
- Add any fields you need
- No coding required

✅ **Real-Time Integration**
- Instant webhook delivery
- 22 ready-to-use presets
- Template system for flexibility

✅ **Gun-Style Scanning**
- Quick Scan Mode for rapid processing
- Beep on scan for audio feedback
- Optimized for speed

✅ **Complete Data Management**
- Full history tracking with payloads
- Per-item analytics
- Export to any format

---

## ✨ Features

### 🚀 Scanning Modes

#### Normal Scan Mode
Perfect for detailed data entry and verification:
1. Scan barcode
2. Confirm code is correct
3. Choose action (Log/Update/Create)
4. Select item type
5. Choose fields to update
6. Enter data
7. Select send method
8. Done!

#### Quick Scan Mode 🔥
**Gun-style workflow** for rapid scanning:
1. Enable Quick Scan toggle
2. Select preset
3. Scan → ✓ Beep → Send → Ready
4. Repeat indefinitely

**Perfect for:**
- Stock taking
- Rapid inventory checks
- Receiving shipments
- Warehouse operations

### 📦 Three Built-In Item Types

#### 1️⃣ **Inventory** (13 fields)
Complete product tracking:
- Product info (name, image, category, version, group)
- Pricing (cost, floor, target) in dual currency
- Stock levels (quantity added/removed)
- Storage & notes

#### 2️⃣ **Packaging** (10 fields)
Supplier and materials management:
- Item and supplier details
- Unit quantities and pricing
- Reorder tracking (last ordered date)
- Supplier links and notes

#### 3️⃣ **Shipment** (15 fields) 🆕
Complete shipping lifecycle:
- Tracking and buyer information
- Dates (shipped, estimated delivery)
- Dimensions (weight, height, width, depth)
- Dual measurements (metric/imperial)
- Costs and customs values
- Fulfillment tracking

### 🎯 22 Ready-to-Use Presets

**Automatically created** on first setup with proper schemas:

**Inventory Actions:**
- Create, Update, Add Inventory, Remove Inventory, Sale, Return

**Packaging Actions:**
- Create, Update, Add Inventory, Remove Inventory, Usage

**Shipment Status Tracking:**
- Create, Update, Preparing, Ready to Ship, Out for Pickup, Dropped Off, In Transit, Received, Returned, Rejected, Return To Sender

All presets include:
- Proper field schemas
- Pre-set scanReason values
- Quantity defaults where appropriate
- Full webhook integration

### 🌐 Webhook Integration

**Send Methods:**
1. **Send full payload**: All selected fields with current values
2. **Send with preset**: Use template with placeholders
3. **Send with previous config**: Repeat last send

**Template Variables:**
- `{{code}}`: Scanned barcode
- `{{scanQuantity}}`: User-entered quantity
- `{{timestamp}}`: Unix timestamp (milliseconds)

**Features:**
- Multiple webhook endpoints
- Custom HTTP headers (JSON)
- Payload templates
- Error handling with user feedback
- Retry via history

### 📊 History & Analytics

**Scan History:**
- Last 100 scans with full payloads
- Status tracking (sent/pending)
- Timestamp (12h/24h format)
- Webhook/preset association
- Quick actions (resend, share, delete)

**Item Analytics:**
- Total scan count per item
- Last scanned timestamp
- Complete scan history
- Historical payload viewing
- Trend analysis ready

### 📤 Export Everything

**Export Options:**
- Full CSV history (all 100 scans + payloads)
- Individual scan (text/CSV/JSON)
- Item data (JSON/CSV/text)
- Payload files (JSON)

**Share Anywhere:**
- Email, Slack, Teams
- Google Drive, Dropbox
- Note apps (Notion, Evernote)
- Any app with file sharing

### 🔍 Search Integration

**11 Search Providers:**
Google • DuckDuckGo • Bing • Naver • Daum • eBay • Target • Reddit • Brave • Yandex • Custom

**Search Access:**
- From main screen (last scanned)
- From item details (any item)
- Provider selector per search

### 💱 Currency & Measurements

**Currency:**
- Dual tracking (local + global)
- 5 currencies: KRW, USD, EUR, JPY, CNY
- Symbol or label display
- Three price points per item

**Measurements:**
- Weight: kg / lbs
- Dimensions: cm / inches
- Dual format storage
- System preference toggle

### ⚙️ Configuration

**First-Time Setup Wizard:**
- Guided onboarding
- Webhook creation
- Auto-preset generation
- Skip option available

**Settings:**
- Item Types Management
- Webhook Configuration
- Preset Management
- Search Settings
- Time Format (12h/24h)
- Currency Settings
- Measurement System
- Item Browser

**Developer Tools** (Hidden):
- Re-seed types/presets
- Force migrations
- Clear all data
- Reset setup wizard
- *Unlock: Long-press "Manage Types"*

---

## 💾 Installation

### Requirements
- Android 7.0 (API 24) or higher
- Camera for scanning
- Internet for webhooks
- ~20MB storage

### From Source

```bash
git clone https://github.com/mrnajiboy/he-barcode-scanner.git
cd he-barcode-scanner
# Open in Android Studio and run
```

### APK Download
Download from [Releases](https://github.com/mrnajiboy/he-barcode-scanner/releases/tag/v1.0.0)

---

## 🚀 Quick Start

### First Launch

1. **Setup Wizard** appears automatically
2. **Create webhook** (or skip)
   - Enter name: "My API"
   - Enter URL: "https://api.example.com/webhook"
   - Tap "Create & Continue"
3. **22 presets created** automatically
4. **Start scanning!**

### First Scan (Normal Mode)

1. Tap **SCAN**
2. Point at barcode
3. Confirm code is correct
4. Choose **Create SKU**
5. Select **Inventory**
6. Check fields to fill
7. Enter data
8. Choose **Send with preset**
9. Select "Inventory - Create"
10. Done!

### Quick Scan Setup

1. Toggle **Quick Scan** on main screen
2. Tap **Preset** button
3. Select preset (e.g., "Inventory - Sale")
4. Tap **SCAN**
5. Scan → Beep → Auto-send → Ready for next!

---

## ⚡ Scanning Modes

### Normal Mode (Default)

**Best for:**
- Creating new items
- Detailed data entry
- Verification workflows
- Complex updates

**Flow:**
```
Scan → Confirm → Choose Action → Select Type → Pick Fields → Enter Data → Send
```

### Quick Scan Mode 🔥

**Best for:**
- Rapid inventory checks
- Receiving shipments
- Stock counting
- Point of sale

**Flow:**
```
Scan → Auto-send → Ready (2 seconds total)
```

**Features:**
- No confirmation needed
- Preset auto-selected
- Instant send to webhook
- Beep on success
- Immediate re-scan ready
- Perfect for scan guns

---

## 🎯 Default Presets

### Auto-Created on Setup

All 22 presets include full field schemas and are ready to use immediately:

#### Inventory Presets (6)

| Name | Quan Added | Quan Removed | Scan Reason |
|------|------------|--------------|-------------|
| Inventory - Create | 1 | - | Create |
| Inventory - Update | - | - | Update |
| Inventory - Add Inventory | 1 | - | Add Inventory |
| Inventory - Remove Inventory | - | 1 | Remove Inventory |
| Inventory - Sale | - | 1 | Sale |
| Inventory - Return | 1 | - | Return |

#### Packaging Presets (5)

| Name | Unit Added | Unit Removed | Scan Reason |
|------|------------|--------------|-------------|
| Packaging - Create | 1 | - | Create |
| Packaging - Update | - | - | Update |
| Packaging - Add Inventory | 1 | - | Add Inventory |
| Packaging - Remove Inventory | - | 1 | Remove Inventory |
| Packaging - Usage | - | 1 | Usage |

#### Shipment Presets (11)

All shipment tracking states:
- Create, Update, Preparing, Ready to Ship, Out for Pickup
- Dropped Off, In Transit, Received
- Returned, Rejected, Return To Sender

---

## 📦 Item Types

### Inventory

**Purpose:** Product and stock management

**Key Fields:**
- Product identification (name, category, version, group)
- Pricing (cost, floor, target) - dual currency
- Stock tracking (quantity added/removed)
- Storage locations and notes

**Use Cases:**
- Retail inventory
- Warehouse stock
- Product catalog
- Asset tracking

---

### Packaging

**Purpose:** Supplier and materials management

**Key Fields:**
- Item and supplier details
- Unit quantities and costs
- Reorder tracking
- Supplier links

**Use Cases:**
- Packaging materials
- Supply chain management
- Reorder workflows
- Supplier catalog

---

### Shipment

**Purpose:** Complete shipping lifecycle tracking

**Key Fields:**
- Tracking and buyer info
- Shipping dates
- Dimensions (weight, H/W/D)
- Costs and customs
- Handler tracking

**Use Cases:**
- Order fulfillment
- Shipping workflow
- Customs documentation
- Delivery tracking
- Returns processing

---

## ⚙️ Configuration

### Webhook Setup

**Create in app:**
```
Settings → Manage Webhooks → Add Webhook
```

**Or use first-time wizard:**
- Automatic on first launch
- Creates webhook + 22 presets
- Ready to scan immediately

**Advanced:**
- Add custom headers for auth
- Use payload templates
- Configure multiple endpoints

### Custom Type Creation

**Example: Create "Returns" type:**

1. Settings → Manage Types → NEW TYPE
2. Enter name: "Returns"
3. Enter ID: "returns"
4. Tap type to add fields:
   - returnReason (String, required)
   - originalOrderId (String)
   - customerName (String)
   - refundAmount (Currency)
   - restockable (Boolean)
5. Done!

Now "Returns" appears alongside Inventory/Packaging/Shipment.

---

## 📖 Usage Guide

### Normal Scanning Workflow

**Create New Item:**
```
SCAN → Confirm → Create SKU → Select Type → 
Choose Fields → Fill Data → Send Full Payload → Select Webhook
```

**Update Existing:**
```
SCAN → Confirm → Update SKU → Select Type → 
Choose Fields (pre-filled) → Modify Data → Send with Preset
```

**Log Only:**
```
SCAN → Confirm → Log Only
```
(Saves to history without creating item)

### Quick Scan Workflow

**Setup once:**
1. Toggle **Quick Scan** ON
2. Tap **Preset**
3. Select "Inventory - Sale"

**Then forever:**
```
SCAN → ✓ → (auto-sent) → Ready for next
```

**Tips:**
- Use for repetitive tasks
- Select most-used preset
- Enable beep for confirmation
- Perfect with phone mount/holder

### History Management

**View Details:**
- Tap any scan in history
- See full metadata
- View exact payload sent

**Actions:**
- **More**: Resend or delete
- **View Payload**: See JSON
- **Share**: Export data

**Resend:**
- Choose any preset
- Quantity prompt
- Instant send

### Item Management

**Browse:**
```
Settings → Manage Items → Select Type (dropdown)
```

**View Details:**
- Tap item
- See all fields
- View scan count
- See last scanned time

**Actions:**
- Edit (scan again)
- Delete (with confirmation)
- Resend last payload
- Search web
- Export (JSON/CSV/text)
- View complete history

---

## 📊 Data Formats

### Inventory Payload Example

```json
{
  "code": "9788932627168",
  "scanQuantity": 1,
  "timestamp": 1739340000000,
  "itemType": "Inventory",
  "scanReason": "Stock Check",
  "itemName": "Wireless Headphones",
  "category": "Electronics",
  "version": "v2.1",
  "group": "Audio",
  "storageLocations": "Warehouse A, Shelf 3B",
  "quantityAdded": 50,
  "quantityRemoved": 12,
  "costPerUnit": [{
    "localUnit": {
      "localValue": 89000,
      "localCurrency": "KRW",
      "localSymbol": "₩"
    },
    "globalUnit": {
      "globalValue": 69.99,
      "globalCurrency": "USD",
      "globalSymbol": "$"
    }
  }],
  "floorPrice": [{
    "localUnit": {"localValue": 120000, "localCurrency": "KRW", "localSymbol": "₩"},
    "globalUnit": {"globalValue": 94.99, "globalCurrency": "USD", "globalSymbol": "$"}
  }],
  "targetPrice": [{
    "localUnit": {"localValue": 150000, "localCurrency": "KRW", "localSymbol": "₩"},
    "globalUnit": {"globalValue": 119.99, "globalCurrency": "USD", "globalSymbol": "$"}
  }],
  "notes": "Premium model, check condition on receipt"
}
```

### Shipment Payload Example

```json
{
  "code": "SHIP20260211001",
  "scanQuantity": 1,
  "timestamp": 1739340000000,
  "itemType": "Shipment",
  "scanReason": "In Transit",
  "trackingNumber": "1Z999AA10123456784",
  "buyerName": "John Smith",
  "buyerCountry": "USA",
  "shippedDate": "2026-02-10 14:30",
  "estDeliveryDate": "2026-02-15 17:00",
  "fulfillmentLocation": "Seoul Distribution Center",
  "lastHandledBy": "Warehouse Team A",
  "weight": [{
    "metric": {"value": 2.5, "unit": "kg", "symbol": "kg"},
    "imperial": {"value": 5.5, "unit": "lbs", "symbol": "lbs"}
  }],
  "height": [{
    "metric": {"value": 30, "unit": "cm", "symbol": "cm"},
    "imperial": {"value": 11.8, "unit": "in", "symbol": "in"}
  }],
  "width": [{
    "metric": {"value": 25, "unit": "cm", "symbol": "cm"},
    "imperial": {"value": 9.8, "unit": "in", "symbol": "in"}
  }],
  "depth": [{
    "metric": {"value": 15, "unit": "cm", "symbol": "cm"},
    "imperial": {"value": 5.9, "unit": "in", "symbol": "in"}
  }],
  "shippingCost": [{
    "localUnit": {"localValue": 15000, "localCurrency": "KRW", "localSymbol": "₩"},
    "globalUnit": {"globalValue": 11.50, "globalCurrency": "USD", "globalSymbol": "$"}
  }],
  "declaredCustomsValue": [{
    "localUnit": {"localValue": 120000, "localCurrency": "KRW", "localSymbol": "₩"},
    "globalUnit": {"globalValue": 95.00, "globalCurrency": "USD", "globalSymbol": "$"}
  }],
  "notes": "Fragile - handle with care"
}
```

### Preset Template Example

```json
{
  "code": "{{code}}",
  "scanQuantity": "{{scanQuantity}}",
  "timestamp": "{{timestamp}}",
  "itemType": "Inventory",
  "scanReason": "Sale",
  "quantityRemoved": 1,
  "location": "Store POS",
  "cashier": "terminal-03"
}
```

---

## 🔌 API Integration

### Webhook Endpoint Requirements

**Must accept:**
- POST request
- Content-Type: application/json
- Body: JSON payload

**Should return:**
- 2xx status for success
- Error message in response body for failures

### Node.js Example

```javascript
app.post('/webhook/scan', (req, res) => {
  const { code, scanQuantity, timestamp, itemType, scanReason } = req.body;
  
  console.log(`Scan: ${code} (${itemType}) - ${scanReason}`);
  
  // Your logic here
  database.updateItem(code, req.body);
  
  res.json({ success: true, processed: timestamp });
});
```

### Python Example

```python
@app.route('/webhook/scan', methods=['POST'])
def handle_scan():
    data = request.json
    code = data['code']
    scan_reason = data.get('scanReason', 'Unknown')
    
    print(f"Scan: {code} - {scan_reason}")
    
    # Your logic
    db.update(code, data)
    
    return jsonify({'success': True})
```

### Authentication

**API Key:**
```json
{
  "X-API-Key": "your-secret-key"
}
```

**Bearer Token:**
```json
{
  "Authorization": "Bearer eyJhbGc..."
}
```

Configure in: `Settings → Manage Webhooks → Edit → Custom Headers`

---

## 📤 Export & Sharing

### CSV Export Example

```csv
code,timestamp,formattedTimestamp,presetId,presetName,webhookUrl,webhookName,sent,payload
"9788932627168",1739340000000,"2026-02-11 14:23","preset_123","Inventory - Sale","https://api.example.com/scan","Production API",true,"{""code"":""9788932627168"",""scanQuantity"":1,""scanReason"":""Sale""}"
```

**Contains:**
- Complete scan metadata
- Full JSON payloads (escaped)
- Human-readable timestamps
- Webhook information

### JSON Export Example

```json
{
  "code": "9788932627168",
  "type": "inventory",
  "historyCount": 15,
  "lastScanned": "2026-02-11 14:23",
  "itemName": "Wireless Headphones",
  "category": "Electronics",
  "quantityAdded": 50,
  "quantityRemoved": 12,
  "costPerUnit": { ... },
  "notes": "Premium model"
}
```

---

## 🔧 Technical Details

### Stack
- **Language**: Kotlin 1.9+
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **HTTP**: OkHttp 4.11.0
- **Barcode**: Quickie 1.7.0 (MLKit)
- **Storage**: SharedPreferences (JSON)

### Architecture

**Pattern**: Activity-based MVC
**Storage**: Singleton stores with JSON
**Navigation**: Intent-based
**File Sharing**: FileProvider (secure)

### Data Persistence

**Files:**
- `history`: Last 100 scans
- `items`: All item data (inventory/packaging/shipment/generic)
- `item_types`: Type schemas
- `webhook_configs`: Endpoints & headers
- `presets`: Templates
- `*_settings`: App configuration

**Size:** < 5MB typical for 100 scans + 1000 items

### Performance

- Scan: < 1 second
- Webhook: 30s timeout
- Export: < 1 second (100 items)
- History load: Instant (cached)

---

## 📚 FAQ

**Q: Can I use this offline?**  
A: Yes! Scans save locally. Webhooks fail gracefully. Resend when back online.

**Q: How many items can I track?**  
A: Thousands. Export regularly if you exceed 10,000 for best performance.

**Q: Can I import data?**  
A: Not yet. Planned for v1.1.

**Q: Does it work with Bluetooth scanners?**  
A: Not yet. Camera only for v1.0. Bluetooth planned for v1.2.

**Q: Can I backup my data?**  
A: Yes! Export full history CSV regularly. Also use Android backup.

**Q: What if my webhook goes down?**  
A: Scans save to history with "pending" status. Resend later from history.

**Q: Can I customize the beep sound?**  
A: Currently uses system beep. Custom sounds planned for future release.

**Q: How do I update a preset?**  
A: Settings → Manage Presets → Tap preset → Edit

**Q: Can I delete the default presets?**  
A: Yes! They're regular presets. Recreate via Dev Tools → Re-seed Presets.

---

## 🗺️ Roadmap

### v1.1 (Q2 2026)
- [ ] Data import (CSV/JSON)
- [ ] Offline queue with auto-sync
- [ ] Batch scanning mode
- [ ] Dark theme
- [ ] Scheduled exports

### v1.2 (Q3 2026)
- [ ] Bluetooth scanner support
- [ ] NFC tag support
- [ ] Multi-user/profiles
- [ ] Cloud sync (optional)
- [ ] Advanced filters

### v1.3 (Q4 2026)
- [ ] Custom reports
- [ ] Dashboard/analytics view
- [ ] Barcode generation
- [ ] Label printing
- [ ] API client mode

### v2.0 (2027)
- [ ] SQLite database
- [ ] Jetpack Compose UI
- [ ] Widget support
- [ ] Wear OS companion
- [ ] Enterprise SSO

---

## 📧 Support

**Issues**: [GitHub Issues](https://github.com/mrnajiboy/he-barcode-scanner/issues)  
**Discussions**: [GitHub Discussions](https://github.com/mrnajiboy/he-barcode-scanner/discussions)  

---

## 🙏 Acknowledgments

- **Quickie** - Excellent MLKit wrapper for barcode scanning
- **OkHttp** - Reliable HTTP client
- **Material Design** - UI/UX guidelines
- **Android Open Source Project** - Platform & tools

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file.

Copyright © 2026 Kenyatta Naji Johnson-Adams

---

## 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=mrnajiboy/he-barcode-scanner&type=Date)](https://star-history.com/#mrnajiboy/he-barcode-scanner&Date)

---

**Built with ❤️ for warehouse workers, retailers, and logistics professionals**

**Version 1.0.0** • February 11, 2026

[⬆ Back to Top](#-he-barcode-scanner)