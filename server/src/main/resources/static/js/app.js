const { createApp } = Vue;

// Constants
const WS_RECONNECT_INTERVAL_MS = 3000;
const MAX_DATA_ITEMS = 100;
const MESSAGE_TYPES = {
    CONNECTIONS_LIST: 'connections_list',
    NETWORK: 'network',
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
            networkData: [],
            databaseData: [],
            logcatData: [],
            expandedItems: new Set()
        };
    },
    mounted() {
        this.connectWebSocket();
        window.addEventListener('beforeunload', this.handleBeforeUnload);
    },
    beforeUnmount() {
        this.cleanup();
    },
    methods: {
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
                console.log('Received message:', message);
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
            console.log('Updated connections:', this.connections);
        },

        handleNetworkData(message) {
            if (this.shouldDisplayData(message.connectionId)) {
                this.addDataItem(this.networkData, message.data);
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
            dataArray.unshift(item);
            if (dataArray.length > MAX_DATA_ITEMS) {
                dataArray.splice(MAX_DATA_ITEMS);
            }
        },

        onConnectionChange() {
            console.log('Selected connection:', this.selectedConnection);
            this.clearAllData();
            this.sendConnectionSelection();
        },

        clearAllData() {
            this.networkData = [];
            this.databaseData = [];
            this.logcatData = [];
            this.expandedItems.clear();
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

        toggleExpand(index) {
            if (this.expandedItems.has(index)) {
                this.expandedItems.delete(index);
            } else {
                this.expandedItems.add(index);
            }
            // Force reactivity update for Set
            this.expandedItems = new Set(this.expandedItems);
        },

        getStatusClass(code) {
            if (code >= 200 && code < 300) return 'success';
            if (code >= 300 && code < 400) return 'redirect';
            if (code >= 400 && code < 500) return 'client-error';
            if (code >= 500) return 'server-error';
            return '';
        },

        formatHeaders(headers) {
            if (!headers) return '';
            return Object.entries(headers)
                .map(([key, value]) => `${key}: ${Array.isArray(value) ? value.join(', ') : value}`)
                .join('\n');
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
                    } else {
                        console.error('Shutdown failed:', response.status);
                    }
                })
                .catch(error => {
                    console.error('Error shutting down server:', error);
                });
        },

        handleBeforeUnload() {
            this.exitServer();
        },

        cleanup() {
            this.disconnectWebSocket();
            window.removeEventListener('beforeunload', this.handleBeforeUnload);
        }
    }
}).mount('#app');
