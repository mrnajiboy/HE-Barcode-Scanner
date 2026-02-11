# Changelog

All notable changes to HE Barcode Scanner will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-02-11

### 🎉 Initial Release

A complete barcode scanning solution with advanced inventory management, webhook integration, and enterprise-grade features.

---

### 📱 Scanning & Workflows

#### Added
- **Multi-format barcode scanning**: QR Code, Aztec, Data Matrix, EAN-8, EAN-13, UPC-A, UPC-E, Code 39, Code 93, Code 128, ITF
- **Scan confirmation dialog**: Verify scanned code before processing (prevents mis-scans)
- **Quick Scan Mode**: Rapid gun-style scanning workflow
  - Toggle on main screen
  - Select preset for automatic sending
  - Scan → Beep → Send → Ready for next scan
  - Perfect for warehouse/retail rapid scanning
- **Audible feedback**: Toggleable beep sound on successful scan
- **Three workflow options**: 
  - Log Only (no item creation)
  - Update SKU (modify existing)
  - Create SKU (new item)

---

### 📦 Item Types & Fields

#### Built-in Item Types (3)

**Inventory:**
- Item Name, Image URL, Category, Version, Group
- Scan Reason, Storage Locations, Notes
- Quantity Added/Removed
- Currency Fields: Cost Per Unit, Floor Price, Target Price

**Packaging:**
- Item, Supplier, Scan Reason
- Quantity Per Unit, Unit Quantity Added/Removed
- Last Ordered (Date/Time), Supplier Link, Notes
- Currency Field: Cost Per Unit

**Shipment:** *(NEW)*
- Tracking Number, Buyer Name, Buyer Country
- Shipped Date, Est. Delivery Date
- Fulfillment Location, Last Handled By, Scan Reason
- Measurement Fields: Weight (kg/lbs), Height/Width/Depth (cm/inches)
- Currency Fields: Shipping Cost, Declared Customs Value
- Notes

#### Custom Types
- **Unlimited custom types**: Create your own schemas
- **7 Field Types**:
  - String: Text data
  - Number: Integers and decimals
  - Date/Time: Dual picker (date then time)
  - Boolean: Checkbox
  - Currency: Local + Global pricing
  - Measurement (Weight): Metric (kg) + Imperial (lbs)
  - Measurement (Dimension): Metric (cm) + Imperial (inches)
- **Dynamic field management**: Add/edit/remove fields anytime
- **Universal support**: All types work identically (no special code needed)
- **Field validation**: Mark fields as required

---

### 🌐 Webhooks & Presets

#### Webhook System
- **Multiple endpoints**: Configure unlimited webhooks
- **Custom headers**: JSON format for API keys, auth tokens, etc.
- **Payload templates**: Use `{{code}}`, `{{scanQuantity}}`, `{{timestamp}}` placeholders
- **Previous config memory**: Quick re-send with last configuration

#### 22 Default Presets (Auto-created)

**Inventory (6 presets):**
1. Inventory - Create (quantityAdded: 1, scanReason: Create)
2. Inventory - Update (scanReason: Update)
3. Inventory - Add Inventory (quantityAdded: 1, scanReason: Add Inventory)
4. Inventory - Remove Inventory (quantityRemoved: 1, scanReason: Remove Inventory)
5. Inventory - Sale (quantityRemoved: 1, scanReason: Sale)
6. Inventory - Return (quantityAdded: 1, scanReason: Return)

**Packaging (5 presets):**
1. Packaging - Create (unitQuantityAdded: 1, scanReason: Create)
2. Packaging - Update (scanReason: Update)
3. Packaging - Add Inventory (unitQuantityAdded: 1, scanReason: Add Inventory)
4. Packaging - Remove Inventory (unitQuantityRemoved: 1, scanReason: Remove Inventory)
5. Packaging - Usage (unitQuantityRemoved: 1, scanReason: Usage)

**Shipment (11 presets):**
1. Shipment - Create
2. Shipment - Update
3. Shipment - Preparing
4. Shipment - Ready to Ship
5. Shipment - Out for Pickup
6. Shipment - Dropped Off
7. Shipment - In Transit
8. Shipment - Received
9. Shipment - Returned
10. Shipment - Rejected
11. Shipment - Return To Sender

---

### 📊 History & Analytics

#### Scan History
- **Last 100 scans** retained with full metadata
- **Full payload storage**: Exact JSON sent per scan
- **Timestamp tracking**: 12h/24h format toggle
- **Status tracking**: Sent vs Pending
- **Webhook association**: Track which endpoint received data
- **Resend capability**: Re-send any historical scan

#### Per-Item Analytics
- **Item Details View**: Dedicated screen per item
- **Scan count**: Total times item was scanned
- **Last scanned**: Timestamp of most recent scan
- **Complete history**: View all scans for specific item
- **Payload access**: View and share historical payloads
- **Quick actions**: Resend, search web, export

---

### 📤 Export & Sharing

#### Export Formats
- **CSV**: Full history or individual items with all fields + payloads
- **JSON**: Structured data for API integration
- **Text**: Human-readable formatted output

#### Export Points
- **Full history**: Main screen → Export/Share History
- **Single scan**: History item → Share → Text/CSV/JSON
- **Item data**: Item details → Export/Share
- **Payload only**: View payload → Share

#### Share Capabilities
- Email, messaging, cloud storage
- Copy/paste support (text selectable)
- FileProvider secure sharing
- Universal Android share sheet

---

### 🔍 Search Integration

#### Multi-Provider Search
- **10+ Search Engines**: Google, DuckDuckGo, Bing, Naver, Daum, eBay, Target, Reddit, Brave, Yandex
- **Custom templates**: Define your own search URL
- **Per-item search**: Search any item from details view
- **Quick search**: Search last scanned code from main screen
- **Provider selector**: Choose engine per search

---

### ⚙️ Settings & Configuration

#### First-Time Setup Wizard
- **Guided onboarding**: Welcome screen with instructions
- **Webhook creation**: Create first webhook during setup
- **Auto-seeding**: 22 default presets created automatically
- **Skip option**: Can skip and configure later
- **Completion summary**: Shows what was created

#### Configuration Screens
- **Item Types**: Create/edit/delete types and fields
- **Webhooks**: Manage endpoints, headers, templates
- **Presets**: Create and manage payload templates
- **Search**: Configure default provider
- **Time Format**: 12h/24h toggle
- **Currency**: Local and global currency codes
- **Measurements**: Metric vs Imperial system
- **Items**: Browse, view, edit all scanned items

#### Developer Tools (Hidden Menu)
- **Re-seed Types**: Recreate default types
- **Re-seed Presets**: Recreate 22 default presets
- **Force Migration**: Run schema migrations manually
- **Clear All Data**: Wipe everything (with confirmation)
- **Reset Setup**: Return to first-time wizard
- **Access**: Long-press "Manage Types" in settings

---

### 💱 Currency & Measurements

#### Currency System
- **Dual tracking**: Local and global prices in parallel
- **5 Currencies**: KRW (₩), USD ($), EUR (€), JPY (¥), CNY (¥)
- **Display modes**: Symbol or Label (Local/Global)
- **Three currency fields**: costPerUnit, floorPrice, targetPrice (inventory)
- **Shipping currency**: shippingCost, declaredCustomsValue (shipment)

#### Measurement System
- **Weight tracking**: Metric (kg) and Imperial (lbs)
- **Dimension tracking**: Metric (cm) and Imperial (inches)
- **Dual format**: Both values stored per field
- **System toggle**: Switch preferred display unit
- **Shipment fields**: Weight, Height, Width, Depth

---

### 🔧 Technical Features

#### Data Management
- **Migration system**: Automatic schema updates with version tracking
- **Generic item support**: Universal handling for custom types
- **Field prefilling**: Auto-populate from previous scans
- **Selective updates**: Choose which fields to modify
- **Cache management**: In-memory caching for fast access
- **Data persistence**: SharedPreferences with JSON serialization

#### User Experience
- **Copy/paste support**: Text selection enabled everywhere
- **Error handling**: User-friendly error messages
- **Confirmation dialogs**: Prevent accidental deletions
- **Toast feedback**: Clear status messages
- **Scroll views**: Handle long field lists
- **Multi-choice dialogs**: Select multiple fields easily

#### Security & Performance
- **FileProvider**: Secure file sharing
- **App sandboxing**: Data isolated per Android security model
- **Webhook timeout**: 30 second timeout prevents hanging
- **History limit**: 100 entries prevents unbounded growth
- **Efficient serialization**: Lazy loading where possible

---

### 📋 Field Management

**Inventory Fields (13):**
- itemName, imageUrl, category, version, group, scanReason
- costPerUnit (currency), floorPrice (currency), targetPrice (currency)
- storageLocations, notes
- quantityAdded, quantityRemoved

**Packaging Fields (10):**
- item, supplier, scanReason
- quantityPerUnit, costPerUnit (currency)
- unitQuantityAdded, unitQuantityRemoved
- lastOrdered (date/time), supplierLink, notes

**Shipment Fields (15):**
- trackingNumber, buyerName, buyerCountry
- shippedDate (date/time), estDeliveryDate (date/time)
- fulfillmentLocation, lastHandledBy, scanReason
- weight (measurement), height (measurement), width (measurement), depth (measurement)
- shippingCost (currency), declaredCustomsValue (currency)
- notes

---

### 🐛 Bug Fixes

- Fixed "Send full payload" sending template instead of actual data
- Fixed CSV exports showing template placeholders instead of real payloads
- Fixed date/time picker only showing date (now shows both)
- Fixed share button not appearing on scan details page
- Fixed payload view showing only last payload per SKU (now shows per-scan)
- Fixed text fields not being selectable for copy/paste
- Fixed custom type fields not appearing in parameter dialog
- Fixed PackagingItem missing scanReason field
- Fixed preset list using clunky text display (now clean row layout)

---

### 🎯 Design Decisions

**Why scanQuantity instead of quantity?**
- Distinguishes scan-time quantity from item quantity fields
- Prevents confusion with quantityAdded/quantityRemoved
- Clearer in payload analysis

**Why dual currency/measurement?**
- International business needs both local and customer currencies
- Measurement systems vary by region
- Both values stored allows flexible reporting

**Why 100 history limit?**
- Prevents unbounded SharedPreferences growth
- 100 is sufficient for most use cases
- Export regularly for longer retention

**Why generic item system?**
- Eliminates need for special code per type
- Unlimited extensibility
- Consistent UX across all types

---

### 📊 Statistics

- **Total Lines of Code**: ~3,500
- **Activities**: 10
- **Data Models**: 12
- **Singletons**: 5 (Settings objects)
- **Data Stores**: 6
- **Layouts**: 14
- **Default Presets**: 22
- **Supported Barcodes**: 11 formats
- **Search Providers**: 11
- **Currencies**: 5
- **Field Types**: 7

---

### 🚀 What's Next?

See [Roadmap](#️-roadmap) in README for planned features.

---

**Version 1.0.0** represents a complete, production-ready barcode scanning and inventory management solution suitable for small to medium businesses, warehouse operations, retail environments, and custom tracking needs.
