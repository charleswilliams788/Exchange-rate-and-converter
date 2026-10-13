package com.example.data.model

enum class BlockchainType {
    EVM,
    SOLANA,
    UNSUPPORTED
}

data class CryptoNetworkOption(
    val id: String,
    val name: String,
    val shortName: String,
    val blockchainType: BlockchainType,
    val receivingAddress: String,
    val minDepositDisplay: String,
    val minAmountNumeric: Double,
    val estimatedFee: String,
    val estimatedTime: String,
    val blockConfirmations: Int,
    val isEnabled: Boolean = true
)

data class EvmNetwork(
    val id: String,
    val name: String,
    val shortName: String,
    val chainId: Int,
    val gasFeeEstimate: String,
    val blockConfirmations: Int,
    val speed: String
)

data class CryptoAsset(
    val symbol: String,
    val name: String,
    val iconEmoji: String,
    val defaultNetworkId: String,
    val defaultNetwork: String = "BNB Smart Chain (BEP-20)",
    val supportedNetworks: List<String> = listOf("BNB Smart Chain (BEP-20)", "Ethereum Mainnet (ERC-20)"),
    val minTransferAmount: Double = 1.0,
    val networks: List<CryptoNetworkOption> = emptyList()
)

object EvmWalletConfig {
    const val DEMO_EVM_WALLET_ADDRESS = "0xe8c91f657af4706e570c8fbb396238db60d136da"
    const val DEMO_SOLANA_WALLET_ADDRESS = "24RXeCxvZmfoo9gGMvhVM9dpf5m4yyXf9R8T3WdQmmvi"

    // Backward-compatibility alias
    const val DEFAULT_EVM_WALLET_ADDRESS = DEMO_EVM_WALLET_ADDRESS

    val NETWORKS = listOf(
        EvmNetwork(
            id = "bsc",
            name = "BNB Smart Chain (BEP-20)",
            shortName = "BNB Chain",
            chainId = 56,
            gasFeeEstimate = "~$0.15 (0.0003 BNB)",
            blockConfirmations = 15,
            speed = "~3 seconds"
        ),
        EvmNetwork(
            id = "ethereum",
            name = "Ethereum Mainnet (ERC-20)",
            shortName = "Ethereum",
            chainId = 1,
            gasFeeEstimate = "~$1.85 (0.0007 ETH)",
            blockConfirmations = 12,
            speed = "~15 seconds"
        ),
        EvmNetwork(
            id = "arbitrum",
            name = "Arbitrum One (EVM L2)",
            shortName = "Arbitrum",
            chainId = 42161,
            gasFeeEstimate = "~$0.10 (0.00004 ETH)",
            blockConfirmations = 20,
            speed = "~1 second"
        ),
        EvmNetwork(
            id = "polygon",
            name = "Polygon (POL / ERC-20)",
            shortName = "Polygon",
            chainId = 137,
            gasFeeEstimate = "~$0.01 (0.02 POL)",
            blockConfirmations = 30,
            speed = "~2 seconds"
        )
    )

    val ALL_ASSETS = listOf(
        CryptoAsset(
            symbol = "USDT",
            name = "Tether USD",
            iconEmoji = "🟢",
            defaultNetworkId = "bsc",
            defaultNetwork = "BNB Smart Chain (BEP-20)",
            supportedNetworks = listOf("BNB Smart Chain (BEP-20)", "Ethereum Mainnet (ERC-20)"),
            minTransferAmount = 2.0,
            networks = listOf(
                CryptoNetworkOption(
                    id = "bsc",
                    name = "BNB Smart Chain (BEP-20)",
                    shortName = "BNB Chain",
                    blockchainType = BlockchainType.EVM,
                    receivingAddress = DEMO_EVM_WALLET_ADDRESS,
                    minDepositDisplay = "5.00 USDT",
                    minAmountNumeric = 5.0,
                    estimatedFee = "~$0.20 (Gas in BNB)",
                    estimatedTime = "~1-2 mins",
                    blockConfirmations = 15,
                    isEnabled = true
                ),
                CryptoNetworkOption(
                    id = "ethereum",
                    name = "Ethereum Mainnet (ERC-20)",
                    shortName = "Ethereum",
                    blockchainType = BlockchainType.EVM,
                    receivingAddress = DEMO_EVM_WALLET_ADDRESS,
                    minDepositDisplay = "10.00 USDT",
                    minAmountNumeric = 10.0,
                    estimatedFee = "~$3.00 (Gas in ETH)",
                    estimatedTime = "~3-5 mins",
                    blockConfirmations = 12,
                    isEnabled = true
                ),
                CryptoNetworkOption(
                    id = "trc20",
                    name = "Tron (TRC-20) — Coming Soon",
                    shortName = "Tron",
                    blockchainType = BlockchainType.UNSUPPORTED,
                    receivingAddress = "Unsupported in Demo Mode",
                    minDepositDisplay = "N/A",
                    minAmountNumeric = 0.0,
                    estimatedFee = "N/A",
                    estimatedTime = "N/A",
                    blockConfirmations = 0,
                    isEnabled = false
                )
            )
        ),
        CryptoAsset(
            symbol = "ETH",
            name = "Ethereum",
            iconEmoji = "⟠",
            defaultNetworkId = "ethereum",
            defaultNetwork = "Ethereum Mainnet (ERC-20)",
            supportedNetworks = listOf("Ethereum Mainnet (ERC-20)", "Arbitrum One (EVM L2)"),
            minTransferAmount = 0.001,
            networks = listOf(
                CryptoNetworkOption(
                    id = "ethereum",
                    name = "Ethereum Mainnet (ERC-20 / Native)",
                    shortName = "Ethereum",
                    blockchainType = BlockchainType.EVM,
                    receivingAddress = DEMO_EVM_WALLET_ADDRESS,
                    minDepositDisplay = "0.005 ETH",
                    minAmountNumeric = 0.005,
                    estimatedFee = "~$2.50 (0.0008 ETH)",
                    estimatedTime = "~3-5 mins",
                    blockConfirmations = 12,
                    isEnabled = true
                ),
                CryptoNetworkOption(
                    id = "arbitrum",
                    name = "Arbitrum One (EVM L2)",
                    shortName = "Arbitrum",
                    blockchainType = BlockchainType.EVM,
                    receivingAddress = DEMO_EVM_WALLET_ADDRESS,
                    minDepositDisplay = "0.002 ETH",
                    minAmountNumeric = 0.002,
                    estimatedFee = "~$0.15 (0.00005 ETH)",
                    estimatedTime = "~1 min",
                    blockConfirmations = 20,
                    isEnabled = true
                )
            )
        ),
        CryptoAsset(
            symbol = "BNB",
            name = "BNB",
            iconEmoji = "🟡",
            defaultNetworkId = "bsc",
            defaultNetwork = "BNB Smart Chain (BEP-20)",
            supportedNetworks = listOf("BNB Smart Chain (BEP-20)"),
            minTransferAmount = 0.005,
            networks = listOf(
                CryptoNetworkOption(
                    id = "bsc",
                    name = "BNB Smart Chain (BEP-20 / Native)",
                    shortName = "BNB Chain",
                    blockchainType = BlockchainType.EVM,
                    receivingAddress = DEMO_EVM_WALLET_ADDRESS,
                    minDepositDisplay = "0.02 BNB",
                    minAmountNumeric = 0.02,
                    estimatedFee = "~$0.15 (0.0002 BNB)",
                    estimatedTime = "~1-2 mins",
                    blockConfirmations = 15,
                    isEnabled = true
                )
            )
        ),
        CryptoAsset(
            symbol = "SOL",
            name = "Solana",
            iconEmoji = "🟣",
            defaultNetworkId = "solana",
            defaultNetwork = "Solana Network",
            supportedNetworks = listOf("Solana Network"),
            minTransferAmount = 0.01,
            networks = listOf(
                CryptoNetworkOption(
                    id = "solana",
                    name = "Solana Network (Native)",
                    shortName = "Solana",
                    blockchainType = BlockchainType.SOLANA,
                    receivingAddress = DEMO_SOLANA_WALLET_ADDRESS,
                    minDepositDisplay = "0.05 SOL",
                    minAmountNumeric = 0.05,
                    estimatedFee = "~$0.001 (0.000005 SOL)",
                    estimatedTime = "~15-30 secs",
                    blockConfirmations = 32,
                    isEnabled = true
                )
            )
        ),
        CryptoAsset(
            symbol = "USDC",
            name = "USD Coin",
            iconEmoji = "🔵",
            defaultNetworkId = "arbitrum",
            defaultNetwork = "Arbitrum One (EVM L2)",
            supportedNetworks = listOf("Arbitrum One (EVM L2)", "Ethereum Mainnet (ERC-20)"),
            minTransferAmount = 2.0,
            networks = listOf(
                CryptoNetworkOption(
                    id = "arbitrum",
                    name = "Arbitrum One (EVM L2)",
                    shortName = "Arbitrum",
                    blockchainType = BlockchainType.EVM,
                    receivingAddress = DEMO_EVM_WALLET_ADDRESS,
                    minDepositDisplay = "5.00 USDC",
                    minAmountNumeric = 5.0,
                    estimatedFee = "~$0.10 (Gas in ETH)",
                    estimatedTime = "~1 min",
                    blockConfirmations = 20,
                    isEnabled = true
                ),
                CryptoNetworkOption(
                    id = "ethereum",
                    name = "Ethereum Mainnet (ERC-20)",
                    shortName = "Ethereum",
                    blockchainType = BlockchainType.EVM,
                    receivingAddress = DEMO_EVM_WALLET_ADDRESS,
                    minDepositDisplay = "10.00 USDC",
                    minAmountNumeric = 10.0,
                    estimatedFee = "~$3.00 (Gas in ETH)",
                    estimatedTime = "~3-5 mins",
                    blockConfirmations = 12,
                    isEnabled = true
                ),
                CryptoNetworkOption(
                    id = "solana",
                    name = "Solana Network (SPL)",
                    shortName = "Solana",
                    blockchainType = BlockchainType.SOLANA,
                    receivingAddress = DEMO_SOLANA_WALLET_ADDRESS,
                    minDepositDisplay = "5.00 USDC",
                    minAmountNumeric = 5.0,
                    estimatedFee = "~$0.001 (0.000005 SOL)",
                    estimatedTime = "~15-30 secs",
                    blockConfirmations = 32,
                    isEnabled = true
                )
            )
        )
    )

    val ASSETS: List<CryptoAsset>
        get() = ALL_ASSETS

    fun getAssetBySymbol(symbol: String): CryptoAsset {
        return ALL_ASSETS.find { it.symbol.equals(symbol, ignoreCase = true) } ?: ALL_ASSETS.first()
    }

    fun getNetworkByName(networkName: String): EvmNetwork {
        return NETWORKS.find { it.name.equals(networkName, ignoreCase = true) } ?: NETWORKS.first()
    }

    fun formatTruncatedAddress(address: String): String {
        val trimmed = address.trim()
        if (trimmed.length <= 12) return trimmed
        return "${trimmed.take(6)}...${trimmed.takeLast(4)}"
    }
}
