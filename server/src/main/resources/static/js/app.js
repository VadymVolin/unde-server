const { createApp } = Vue;

// Constants
const WS_RECONNECT_INTERVAL_MS = 3000;
const MAX_DATA_ITEMS = 1000;
const MESSAGE_TYPES = {
    CONNECTIONS_LIST: 'connections_list',
    NETWORK: 'network',
    NETWORKS: 'networks',
    DATABASE: 'database',
    LOGCAT: 'logcat',
    SELECT_CONNECTION: 'select_connection'
};

createApp({
    data() {
        return {
            ws: null,
            wsConnected: false,
            reconnectInterval: null,
            activeTab: 'network',
            selectedConnection: '',
            connections: [],

            // Data Arrays
            networkData: [],
            databaseData: [],
            logcatData: [],

            // UI State
            theme: 'light',
            selectedRequestIndex: null, // Track selected item index
        };
    },
    computed: {
        selectedRequest() {
            if (this.selectedRequestIndex !== null && this.networkData[this.selectedRequestIndex]) {
                return this.networkData[this.selectedRequestIndex];
            }
            return null;
        }
    },
    mounted() {
        this.initTheme();
        this.connectWebSocket();
        window.addEventListener('beforeunload', this.handleBeforeUnload);
    },
    beforeUnmount() {
        this.cleanup();
    },
    methods: {
        // --- Theme Logic ---
        initTheme() {
            const savedTheme = localStorage.getItem('unde-theme') || 'light';
            this.setTheme(savedTheme);
        },
        toggleTheme() {
            const newTheme = this.theme === 'light' ? 'dark' : 'light';
            this.setTheme(newTheme);
        },
        setTheme(themeName) {
            this.theme = themeName;
            document.documentElement.setAttribute('data-theme', themeName);
            localStorage.setItem('unde-theme', themeName);
        },

        // --- Selection Logic ---
        selectRequest(index) {
            this.selectedRequestIndex = index;
        },

        // --- WebSocket Logic ---
        connectWebSocket() {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = `${protocol}//${window.location.host}/ws/client`;

            console.log('Connecting to WebSocket:', wsUrl);
            this.ws = new WebSocket(wsUrl);

            this.ws.onopen = this.handleWebSocketOpen;
            this.ws.onmessage = this.handleWebSocketMessage;
            this.ws.onerror = this.handleWebSocketError;
            this.ws.onclose = this.handleWebSocketClose;
        },

        handleWebSocketOpen() {
            console.log('WebSocket connected');
            this.wsConnected = true;
            this.clearReconnectInterval();
        },

        handleWebSocketMessage(event) {
            try {
                const message = JSON.parse(event.data);
                this.handleMessage(message);
            } catch (error) {
                console.error('Failed to parse message:', error);
            }
        },

        handleWebSocketError(error) {
            console.error('WebSocket error:', error);
        },

        handleWebSocketClose() {
            console.log('WebSocket disconnected');
            this.wsConnected = false;
            this.scheduleReconnect();
        },

        disconnectWebSocket() {
            this.clearReconnectInterval();
            if (this.ws) {
                this.ws.close();
                this.ws = null;
            }
        },

        clearReconnectInterval() {
            if (this.reconnectInterval) {
                clearInterval(this.reconnectInterval);
                this.reconnectInterval = null;
            }
        },

        scheduleReconnect() {
            if (!this.reconnectInterval) {
                this.reconnectInterval = setInterval(() => {
                    console.log('Attempting to reconnect...');
                    this.connectWebSocket();
                }, WS_RECONNECT_INTERVAL_MS);
            }
        },

        handleMessage(message) {
            const { type } = message;

            switch (type) {
                case MESSAGE_TYPES.CONNECTIONS_LIST:
                    this.handleConnectionsList(message);
                    break;

                case MESSAGE_TYPES.NETWORK:
                    this.handleNetworkData(message);
                    break;

                case MESSAGE_TYPES.NETWORKS:
                    this.handleNetworksData(message);
                    break;

                case MESSAGE_TYPES.DATABASE:
                    this.handleDatabaseData(message);
                    break;

                case MESSAGE_TYPES.LOGCAT:
                    this.handleLogcatData(message);
                    break;

                default:
                    console.warn('Unknown message type:', type);
            }
        },

        handleConnectionsList(message) {
            this.connections = message.connections || [];
        },

        handleNetworkData(message) {
            if (this.shouldDisplayData(message.connectionId)) {
                this.addDataItem(this.networkData, message.data);
            }
        },

        handleNetworksData(message) {
            if (this.shouldDisplayData(message.connectionId)) {
                this.addDataItems(this.networkData, message.data);
            }
        },

        handleDatabaseData(message) {
            if (this.shouldDisplayData(message.connectionId)) {
                this.addDataItem(this.databaseData, message.data);
            }
        },

        handleLogcatData(message) {
            if (this.shouldDisplayData(message.connectionId)) {
                this.addDataItem(this.logcatData, message.data);
            }
        },

        shouldDisplayData(connectionId) {
            return connectionId === this.selectedConnection || !this.selectedConnection;
        },

        addDataItem(dataArray, item) {
            dataArray.push(item);
            if (dataArray.length > MAX_DATA_ITEMS) {
                dataArray.splice(MAX_DATA_ITEMS);
            }
        },

        addDataItems(dataArray, items) {
            dataArray.push(...items);
            if (dataArray.length > MAX_DATA_ITEMS) {
                dataArray.splice(MAX_DATA_ITEMS);
            }
        },

        onConnectionChange() {
            this.clearAllData();
            this.sendConnectionSelection();
        },

        clearAllData() {
            this.networkData = [];
            this.databaseData = [];
            this.logcatData = [];
            this.selectedRequestIndex = null;
        },

        sendConnectionSelection() {
            if (this.ws && this.ws.readyState === WebSocket.OPEN) {
                const message = {
                    type: MESSAGE_TYPES.SELECT_CONNECTION,
                    connectionId: this.selectedConnection
                };
                this.ws.send(JSON.stringify(message));
            }
        },

        // --- Formatter Helpers ---
        getStatusClass(code) {
            if (code >= 200 && code < 300) return 'success';
            if (code >= 300 && code < 400) return 'redirect'; // Warning color
            if (code >= 400 && code < 500) return 'error'; // Client error
            if (code >= 500) return 'error'; // Server error
            return '';
        },

        formatTime(timestamp) {
            if (!timestamp) return '';
            const date = new Date(timestamp);
            return date.toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit', fractionalSecondDigits: 3 });
        },

        formatBody(body) {
            if (!body) return 'null';
            try {
                // Try to parse if it's a JSON string to pretty print
                if (typeof body === 'string' && (body.startsWith('{') || body.startsWith('['))) {
                    return JSON.stringify(JSON.parse(body), null, 2);
                }
                return typeof body === 'object' ? JSON.stringify(body, null, 2) : body;
            } catch (e) {
                return body; // Return as is if not valid JSON
            }
        },

        exitServer() {
            fetch('/unde/exit', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            })
                .then(response => {
                    if (response.ok) {
                        console.log('Server shutdown initiated');
                    }
                })
                .catch(error => {
                    console.error('Error shutting down server:', error);
                });
        },

        handleBeforeUnload() {
            //            this.exitServer();
        },

        cleanup() {
            this.disconnectWebSocket();
            window.removeEventListener('beforeunload', this.handleBeforeUnload);
        }
    }
}).mount('#app');
