# Encrypted Bluetooth Chat Application

## Project Overview

A secure, peer-to-peer chat application that operates over Bluetooth with end-to-end encryption for both text messages and voice calls. No internet connection required.

## Key Features

- **Direct Connection**: Bluetooth-only communication, no internet required
- **End-to-End Encryption**: AES-256-GCM with ECDHE key exchange
- **Voice Calls**: High-quality encrypted voice communication
- **Modern UI**: Material Design with Arabic support
- **Advanced Security**: Protection against eavesdropping and attacks
- **Perfect Forward Secrecy**: Session-based key rotation

## Technical Architecture

### Architecture Pattern: MVVM

```
├── View Layer (UI)
│   ├── MainActivity
│   ├── ChatActivity
│   ├── DeviceDiscoveryActivity
│   └── CallActivity
├── ViewModel Layer
│   ├── ChatViewModel
│   ├── ConnectionViewModel
│   └── CallViewModel
├── Model Layer
│   ├── BluetoothManager
│   ├── CryptoManager
│   └── AudioManager
└── Repository Layer
    ├── MessageRepository
    ├── DeviceRepository
    └── CallRepository
```

### Core Components

- **BluetoothManager**: Handles Bluetooth connections and data transfer
- **CryptoManager**: Manages encryption, decryption, and key exchange
- **AudioManager**: Processes audio for voice calls
- **MessageHandler**: Manages message formatting and delivery
- **ConnectionHandler**: Manages device connections and status

## Encryption System

### Cryptographic Algorithm
- **Key Exchange**: ECDHE-P256 (Elliptic Curve Diffie-Hellman Ephemeral)
- **Symmetric Encryption**: AES-256-GCM
- **Authentication**: HMAC-SHA256
- **Perfect Forward Secrecy**: Session-based keys

### Message Structure
```
├── Header (16 bytes)
│   ├── Version (2 bytes)
│   ├── Message Type (2 bytes)
│   ├── Sender ID (4 bytes)
│   ├── Sequence Number (4 bytes)
│   └── Payload Length (4 bytes)
├── Encrypted Payload (Variable)
└── Authentication Tag (16 bytes)
```

## Development Requirements

### Minimum Requirements
- **Android Version**: 6.0 (API 23)
- **Target SDK**: Android 14 (API 34)
- **Permissions**: Bluetooth, Audio recording, Location (for device discovery)
- **Hardware**: Bluetooth support

### Development Tools
- **IDE**: Android Studio
- **Language**: Kotlin
- **Build System**: Gradle
- **Version Control**: Git

### Key Dependencies
- **Encryption**: Bouncy Castle
- **Audio Processing**: OpenSL ES
- **UI Components**: Material Design Components
- **Testing**: JUnit, Espresso

## Security Features

### Data Protection
- **At-Rest Encryption**: AES-256 for stored data
- **In-Transit Encryption**: AES-256-GCM for transmitted data
- **Key Management**: Android Keystore integration
- **Secure Deletion**: Cryptographic erasure of sensitive data

### Attack Resistance
- **Man-in-the-Middle**: Certificate pinning and key verification
- **Replay Attacks**: Sequence number validation
- **Eavesdropping**: Perfect Forward Secrecy
- **Brute Force**: Key stretching and rate limiting

## Audio Specifications

### Audio Quality
- **Sample Rate**: 44.1 kHz
- **Bit Depth**: 16-bit
- **Channels**: Mono
- **Compression**: Opus Codec
- **Latency**: < 150ms

## UI/UX Design

### Color Scheme
- **Primary**: Deep Blue (#1565C0)
- **Secondary**: Light Blue (#42A5F5)
- **Accent**: Success Green (#4CAF50)
- **Error**: Warning Red (#F44336)

### Typography
- **Font**: Roboto/Noto Sans Arabic
- **Headers**: 24sp, Bold
- **Body**: 16sp, Regular
- **Captions**: 14sp, Medium

## Development Phases

### Phase 1 (4 weeks)
- Project setup and architecture
- Basic Bluetooth functionality
- Core encryption implementation
- Basic UI design

### Phase 2 (3 weeks)
- Message system development
- Chat interface implementation
- Advanced encryption features
- Initial security testing

### Phase 3 (3 weeks)
- Voice call system
- Audio quality optimization
- Advanced features
- Performance testing

### Phase 4 (2 weeks)
- Comprehensive testing
- Performance optimization
- Bug fixes
- Release preparation

## Testing Strategy

### Unit Testing
- Cryptographic functions
- Bluetooth operations
- Message handling
- Audio processing

### Integration Testing
- End-to-end communication
- Cross-device compatibility
- Security validation
- Performance benchmarking

## Performance Optimization

### Memory Management
- Object pooling
- Lazy loading
- Memory leak prevention
- Garbage collection optimization

### Battery Optimization
- Doze mode compatibility
- Background process optimization
- Smart connection management
- Power-efficient scanning

## Legal and Ethical Considerations

### Compliance
- Export regulations for encryption
- Local privacy laws
- Data protection requirements
- User consent mechanisms

### Ethics
- Privacy by design
- User transparency
- Responsible use guidelines
- Ethical AI principles

## Installation and Setup

1. Clone the repository
2. Open in Android Studio
3. Install required dependencies
4. Configure Bluetooth permissions
5. Build and run on device

## Contributing

Please read CONTRIBUTING.md for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please refer to the documentation or open an issue in the repository.