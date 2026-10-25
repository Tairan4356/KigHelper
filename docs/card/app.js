(function () {
    "use strict";

    const PLATFORM_NAMES = [
        "自定义",
        "QQ",
        "微信",
        "哔哩哔哩",
        "抖音",
        "微博",
        "X",
        "Facebook",
        "Instagram",
        "Telegram"
    ];

    const PLATFORM_COLORS = [
        "#f3b8d0",
        "#8fc4ff",
        "#8fe3a8",
        "#ffa8cd",
        "#c7b5ff",
        "#ffc083",
        "#e5e9f2",
        "#a9c7ff",
        "#ffb0c2",
        "#9cddf7"
    ];

    const PLATFORM_ICONS = {
        1: "./icons/qq.svg",
        2: "./icons/wechat.svg",
        3: "./icons/bilibili.svg",
        4: "./icons/douyin.svg",
        5: "./icons/weibo.svg",
        6: "./icons/x.svg",
        7: "./icons/facebook.svg",
        8: "./icons/instagram.svg",
        9: "./icons/telegram.svg"
    };

    const ICONS = {
        open: '<i class="fas fa-arrow-up-right-from-square text-xs" aria-hidden="true"></i>',
        copy: '<i class="far fa-copy text-xs" aria-hidden="true"></i>',
        link: '<i class="fas fa-link text-[10px]" aria-hidden="true"></i>',
        empty: '<i class="far fa-note-sticky text-2xl" aria-hidden="true"></i>'
    };

    const CLASS = {
        contact: "group relative flex min-w-0 flex-col gap-4 overflow-hidden rounded-[1.75rem] border border-neutral-200 p-4 shadow-sm transition duration-200 hover:-translate-y-0.5 hover:border-neutral-300 hover:shadow-lg animate-m3-contact-enter sm:flex-row sm:items-center sm:p-5",
        contactMain: "flex min-w-0 items-start gap-3 sm:flex-1 sm:items-center",
        contactIcon: "grid h-12 w-12 flex-none -rotate-3 place-items-center overflow-hidden rounded-2xl text-base font-black text-neutral-900 shadow-md",
        contactIconImage: "h-7 w-7 object-contain",
        contactInfo: "min-w-0 flex-1",
        contactTitleRow: "flex flex-wrap items-center gap-2",
        contactName: "text-base font-bold tracking-tight break-words",
        platformChip: "whitespace-nowrap rounded-full bg-neutral-100 px-2 py-0.5 text-[11px] font-semibold text-neutral-500",
        contactHandle: "mt-1.5 truncate font-mono text-xs text-neutral-500",
        contactTarget: "mt-1.5 flex min-w-0 items-center gap-1.5 text-xs text-neutral-500",
        contactActions: "flex items-center gap-2 sm:flex-none",
        actionButton: "inline-flex items-center justify-center gap-2 rounded-full font-bold transition duration-200 active:scale-95",
        actionJump: "h-12 flex-1 px-5 text-sm text-neutral-900 shadow-m3-fab hover:brightness-95 sm:flex-none sm:px-6",
        actionCopy: "h-11 flex-none bg-neutral-100 px-4 text-xs text-neutral-700 hover:bg-neutral-200",
        empty: "grid justify-items-center gap-2 rounded-[1.75rem] border-2 border-dashed border-neutral-200 bg-white px-6 py-9 text-center text-neutral-500",
        emptyIcon: "grid h-14 w-14 place-items-center rounded-2xl bg-pink-100 text-pink-600"
    };

    const elements = {
        loading: document.getElementById("loading"),
        error: document.getElementById("error"),
        errorMessage: document.getElementById("error-message"),
        content: document.getElementById("content"),
        nickname: document.getElementById("nickname"),
        signature: document.getElementById("signature"),
        truncated: document.getElementById("truncated"),
        contactCount: document.getElementById("contact-count"),
        contacts: document.getElementById("contacts"),
        copyButton: document.getElementById("copy-button"),
        shareButton: document.getElementById("share-button"),
        toast: document.getElementById("toast")
    };

    let toastTimer = 0;

    function setToastVisible(visible) {
        const list = elements.toast.classList;
        list.toggle("opacity-0", !visible);
        list.toggle("opacity-100", visible);
        list.toggle("translate-y-4", !visible);
        list.toggle("translate-y-0", visible);
        list.toggle("scale-95", !visible);
        list.toggle("scale-100", visible);
    }

    function showToast(message) {
        elements.toast.textContent = message;
        elements.toast.hidden = false;
        requestAnimationFrame(function () {
            setToastVisible(true);
        });
        window.clearTimeout(toastTimer);
        toastTimer = window.setTimeout(function () {
            setToastVisible(false);
            window.setTimeout(function () {
                elements.toast.hidden = true;
            }, 180);
        }, 2200);
    }

    function showError(message) {
        elements.loading.hidden = true;
        elements.content.hidden = true;
        elements.error.hidden = false;
        elements.errorMessage.textContent = message;
        elements.shareButton.hidden = true;
    }

    function showContent() {
        elements.loading.hidden = true;
        elements.error.hidden = true;
        elements.content.hidden = false;
        elements.shareButton.hidden = typeof navigator.share !== "function";
    }

    function isSafeTargetUrl(value) {
        try {
            const url = new URL(value);
            return url.protocol === "http:" || url.protocol === "https:" || url.protocol === "mailto:";
        } catch (error) {
            return false;
        }
    }

    function createElement(tag, className, text) {
        const element = document.createElement(tag);
        if (className) element.className = className;
        if (text !== undefined) element.textContent = text;
        return element;
    }

    function mixWithWhite(hex, amount) {
        const value = parseInt(hex.slice(1), 16);
        const channel = function (shift) {
            const part = (value >> shift) & 255;
            return Math.round(part * amount + 255 * (1 - amount));
        };
        return "rgb(" + channel(16) + ", " + channel(8) + ", " + channel(0) + ")";
    }

    function copyText(value) {
        if (navigator.clipboard && window.isSecureContext) {
            return navigator.clipboard.writeText(value);
        }
        return new Promise(function (resolve, reject) {
            const textarea = document.createElement("textarea");
            textarea.value = value;
            textarea.setAttribute("readonly", "");
            textarea.style.position = "fixed";
            textarea.style.opacity = "0";
            document.body.appendChild(textarea);
            textarea.select();
            try {
                const copied = document.execCommand("copy");
                document.body.removeChild(textarea);
                if (copied) resolve();
                else reject(new Error("copy failed"));
            } catch (error) {
                document.body.removeChild(textarea);
                reject(error);
            }
        });
    }

    function createCopyButton(value, label) {
        const button = createElement("button", CLASS.actionButton + " " + CLASS.actionCopy);
        button.type = "button";
        button.setAttribute("aria-label", label);
        button.title = label;
        button.innerHTML = ICONS.copy + "<span>" + label + "</span>";
        button.addEventListener("click", function () {
            copyText(value).then(function () {
                showToast("已复制");
            }).catch(function () {
                showToast("复制失败");
            });
        });
        return button;
    }

    function renderContact(contact, index) {
        const platformId = Number.isInteger(contact.platformId) ? contact.platformId : 0;
        const platformName = PLATFORM_NAMES[platformId] || "社交平台";
        const name = contact.displayName || platformName;
        const accent = PLATFORM_COLORS[platformId] || PLATFORM_COLORS[0];
        const linkTarget = contact.target && isSafeTargetUrl(contact.target) ? contact.target : "";
        const copyTarget = contact.target || contact.handle;
        const copyLabel = linkTarget ? "复制链接" : contact.target ? "复制内容" : "复制账号";

        const card = createElement("article", CLASS.contact);
        card.style.animationDelay = Math.min(index * 55, 330) + "ms";
        card.style.backgroundColor = mixWithWhite(accent, 0.12);

        const main = createElement("div", CLASS.contactMain);
        const icon = createElement("div", CLASS.contactIcon);
        icon.setAttribute("aria-hidden", "true");
        const iconFile = PLATFORM_ICONS[platformId];
        if (iconFile) {
            icon.style.backgroundColor = mixWithWhite(accent, 0.2);
            const image = createElement("img", CLASS.contactIconImage);
            image.src = iconFile;
            image.alt = "";
            icon.appendChild(image);
        } else {
            icon.textContent = Array.from(name)[0] || "?";
            icon.style.backgroundColor = accent;
        }

        const info = createElement("div", CLASS.contactInfo);
        const titleRow = createElement("div", CLASS.contactTitleRow);
        titleRow.appendChild(createElement("h3", CLASS.contactName, name));
        if (name !== platformName && !name.toLowerCase().includes(platformName.toLowerCase())) {
            titleRow.appendChild(createElement("span", CLASS.platformChip, platformName));
        }
        info.appendChild(titleRow);

        if (contact.handle) {
            const handle = createElement("p", CLASS.contactHandle, contact.handle);
            handle.title = contact.handle;
            info.appendChild(handle);
        }

        if (contact.target && contact.target !== contact.handle) {
            const target = createElement("p", CLASS.contactTarget);
            target.title = contact.target;
            target.innerHTML = ICONS.link;
            target.appendChild(createElement("span", "truncate", contact.target));
            info.appendChild(target);
        }

        main.appendChild(icon);
        main.appendChild(info);
        card.appendChild(main);

        if (linkTarget || copyTarget) {
            const actions = createElement("div", CLASS.contactActions);

            if (linkTarget) {
                const open = createElement("a", CLASS.actionButton + " " + CLASS.actionJump);
                open.href = linkTarget;
                open.target = "_blank";
                open.rel = "noopener noreferrer";
                open.innerHTML = ICONS.open + "<span>打开链接</span>";
                open.style.backgroundColor = accent;
                actions.appendChild(open);
            }

            if (copyTarget) {
                actions.appendChild(createCopyButton(copyTarget, copyLabel));
            }

            card.appendChild(actions);
        }

        return card;
    }

    function renderEmptyState() {
        const empty = createElement("div", CLASS.empty);
        const icon = createElement("span", CLASS.emptyIcon);
        icon.innerHTML = ICONS.empty;
        icon.setAttribute("aria-hidden", "true");
        empty.appendChild(icon);
        empty.appendChild(createElement("strong", "text-sm font-bold text-neutral-800", "这里还没有联系方式"));
        empty.appendChild(createElement("p", "text-xs leading-relaxed", "请返回原应用添加平台后重新生成卡片。"));
        return empty;
    }

    function render(payload) {
        const nickname = payload.nickname || "未命名卡片";
        document.title = nickname + " · 娃语";
        elements.nickname.textContent = nickname;

        if (payload.signature) {
            elements.signature.textContent = payload.signature;
            elements.signature.hidden = false;
        } else {
            elements.signature.hidden = true;
        }

        elements.truncated.hidden = !(payload.signatureTruncated || payload.nicknameTruncated);
        elements.contactCount.textContent = payload.contacts.length
            ? payload.contacts.length + " 个平台"
            : "暂无平台";
        elements.contacts.replaceChildren();

        if (payload.contacts.length === 0) {
            elements.contacts.appendChild(renderEmptyState());
        } else {
            payload.contacts.forEach(function (contact, index) {
                elements.contacts.appendChild(renderContact(contact, index));
            });
        }

        showContent();
    }

    async function loadCard() {
        elements.loading.hidden = false;
        elements.error.hidden = true;
        elements.content.hidden = true;

        try {
            if (!window.KigCardCodec) throw new Error("卡片解析脚本加载失败");
            const payload = await window.KigCardCodec.decodeFragment(location.hash.slice(1));
            render(payload);
        } catch (error) {
            showError(error && error.message ? error.message : "无法解析卡片内容");
        }
    }

    elements.copyButton.addEventListener("click", function () {
        copyText(location.href).then(function () {
            showToast("卡片链接已复制");
        }).catch(function () {
            showToast("复制失败");
        });
    });

    elements.shareButton.addEventListener("click", function () {
        if (typeof navigator.share !== "function") return;
        navigator.share({
            title: document.title,
            text: "来自娃语的扩列卡片",
            url: location.href
        }).catch(function () {
            // 用户取消分享时不提示。
        });
    });

    window.addEventListener("hashchange", loadCard);
    loadCard();
})();
