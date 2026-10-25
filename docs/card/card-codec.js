(function (root, factory) {
    "use strict";
    const api = factory();
    if (typeof module === "object" && module.exports) {
        module.exports = api;
    }
    root.KigCardCodec = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
    "use strict";

    const MAX_DECOMPRESSED_BYTES = 64 * 1024;
    const MAX_FIELD_BYTES = 16 * 1024;
    const MAX_CONTACTS = 64;
    const SCHEMA_VERSION = 1;

    const PAYLOAD_FLAG_SIGNATURE_TRUNCATED = 1 << 2;
    const PAYLOAD_FLAG_NICKNAME_TRUNCATED = 1 << 3;

    const CRC_TABLE = (function () {
        const table = new Uint32Array(256);
        for (let index = 0; index < 256; index += 1) {
            let value = index;
            for (let bit = 0; bit < 8; bit += 1) {
                value = (value & 1) ? (0xEDB88320 ^ (value >>> 1)) : (value >>> 1);
            }
            table[index] = value >>> 0;
        }
        return table;
    })();

    function crc32(bytes) {
        let crc = 0xFFFFFFFF;
        for (let index = 0; index < bytes.length; index += 1) {
            crc = (crc >>> 8) ^ CRC_TABLE[(crc ^ bytes[index]) & 0xFF];
        }
        return (crc ^ 0xFFFFFFFF) >>> 0;
    }

    function decodeBase64Url(value) {
        if (!value) throw new Error("链接中缺少卡片内容");
        let normalized = value.replace(/-/g, "+").replace(/_/g, "/");
        while (normalized.length % 4 !== 0) normalized += "=";
        const binary = atob(normalized);
        const bytes = new Uint8Array(binary.length);
        for (let index = 0; index < binary.length; index += 1) {
            bytes[index] = binary.charCodeAt(index);
        }
        return bytes;
    }

    async function inflateRaw(compressed) {
        if (typeof DecompressionStream === "function") {
            try {
                const stream = new Blob([compressed]).stream()
                    .pipeThrough(new DecompressionStream("deflate-raw"));
                const reader = stream.getReader();
                const chunks = [];
                let total = 0;
                while (true) {
                    const result = await reader.read();
                    if (result.done) break;
                    total += result.value.length;
                    if (total > MAX_DECOMPRESSED_BYTES) {
                        await reader.cancel();
                        throw new Error("卡片内容过大");
                    }
                    chunks.push(result.value);
                }
                if (total < 5) throw new Error("卡片内容不完整");
                const output = new Uint8Array(total);
                let offset = 0;
                for (const chunk of chunks) {
                    output.set(chunk, offset);
                    offset += chunk.length;
                }
                return output;
            } catch (error) {
                if (error && error.message === "卡片内容过大") throw error;
            }
        }

        const pako = typeof globalThis !== "undefined" ? globalThis.pako : undefined;
        if (pako && typeof pako.inflateRaw === "function") {
            const output = pako.inflateRaw(compressed);
            if (output.length > MAX_DECOMPRESSED_BYTES) {
                throw new Error("卡片内容过大");
            }
            if (output.length < 5) throw new Error("卡片内容不完整");
            return output;
        }

        throw new Error("当前浏览器不支持解析此卡片");
    }

    function parsePayload(bytes) {
        const totalSize = bytes.length;
        const contentSize = totalSize - 4;
        if (contentSize < 5) throw new Error("卡片内容不完整");

        const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
        const expectedCrc = view.getUint32(contentSize, false);
        if (crc32(bytes.subarray(0, contentSize)) !== expectedCrc) {
            throw new Error("CRC 校验失败");
        }

        let index = 0;

        function readByte() {
            if (index >= contentSize) throw new Error("卡片内容不完整");
            return bytes[index++];
        }

        function readVarInt() {
            let shift = 0;
            let value = 0;
            while (shift < 28) {
                const byte = readByte();
                value |= (byte & 0x7F) << shift;
                if ((byte & 0x80) === 0) return value >>> 0;
                shift += 7;
            }
            throw new Error("卡片数据格式错误");
        }

        function readString() {
            const length = readVarInt();
            if (length > MAX_FIELD_BYTES || index + length > contentSize) {
                throw new Error("卡片字段长度异常");
            }
            const value = new TextDecoder("utf-8", { fatal: true })
                .decode(bytes.subarray(index, index + length));
            index += length;
            return value;
        }

        const version = readByte();
        if (version !== SCHEMA_VERSION) throw new Error("不支持的卡片版本");
        const flags = readByte();
        const count = readByte();
        if (count > MAX_CONTACTS) throw new Error("联系方式数量异常");

        const nickname = readString();
        const signature = readString();
        const contacts = [];

        for (let item = 0; item < count; item += 1) {
            const platformId = readByte();
            const contactFlags = readByte();
            const displayName = readString();
            const handle = readString();
            const target = readString();
            contacts.push({
                platformId: platformId,
                displayName: displayName,
                handle: handle,
                target: target,
                targetFromQr: (contactFlags & (1 << 3)) !== 0
            });
        }

        if (index !== contentSize) throw new Error("卡片数据存在多余内容");

        return {
            nickname: nickname,
            signature: signature,
            signatureTruncated: (flags & PAYLOAD_FLAG_SIGNATURE_TRUNCATED) !== 0,
            nicknameTruncated: (flags & PAYLOAD_FLAG_NICKNAME_TRUNCATED) !== 0,
            contacts: contacts
        };
    }

    async function decodeFragment(fragment) {
        const compressed = decodeBase64Url(fragment);
        const inflated = await inflateRaw(compressed);
        return parsePayload(inflated);
    }

    return {
        crc32: crc32,
        decodeBase64Url: decodeBase64Url,
        inflateRaw: inflateRaw,
        parsePayload: parsePayload,
        decodeFragment: decodeFragment
    };
});
