package server

import api.ToastKind

interface MessageSender {
    operator fun invoke(title: String, content: String, kind: ToastKind = ToastKind.INFORMATION)
}