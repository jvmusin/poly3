package server

import api.ToastKind

interface MessageSender {
    operator fun invoke(content: String, kind: ToastKind = ToastKind.INFORMATION)
}