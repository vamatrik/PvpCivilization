package com.pvpciv.game;

import com.pvpciv.net.Network;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/** Серверная система ветвящихся диалогов в стиле визуальной новеллы. */
public final class DialogueManager {
    public record Opt(String label, @Nullable String next) {}
    public record Node(String speaker, int color, String text, List<Opt> opts, @Nullable Consumer<ServerPlayer> onEnter) {}

    public static final class Dialogue {
        final String start;
        final Map<String, Node> nodes = new HashMap<>();
        @Nullable Consumer<ServerPlayer> onEnd;
        @Nullable Consumer<ServerPlayer> onAbort;

        public Dialogue(String start) { this.start = start; }

        public Dialogue node(String id, String speaker, int color, String text, Opt... opts) {
            return node(id, speaker, color, text, null, opts);
        }
        public Dialogue node(String id, String speaker, int color, String text, @Nullable Consumer<ServerPlayer> onEnter, Opt... opts) {
            nodes.put(id, new Node(speaker, color, text, opts.length == 0 ? List.of(new Opt("[Далее]", null)) : List.of(opts), onEnter));
            return this;
        }
        public Dialogue onEnd(Consumer<ServerPlayer> c) { onEnd = c; return this; }
        public Dialogue onAbort(Consumer<ServerPlayer> c) { onAbort = c; return this; }
    }

    public static Opt opt(String label, @Nullable String next) { return new Opt(label, next); }

    private static final class Session {
        final Dialogue d; String node; final int entityId;
        Session(Dialogue d, String node, int entityId) { this.d = d; this.node = node; this.entityId = entityId; }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static boolean inDialogue(ServerPlayer p) { return SESSIONS.containsKey(p.getUUID()); }

    @Nullable
    public static UUID talkerOf(Entity npc) {
        for (var e : SESSIONS.entrySet()) if (e.getValue().entityId == npc.getId()) return e.getKey();
        return null;
    }

    public static void open(ServerPlayer p, Entity speaker, Dialogue d) {
        Session s = new Session(d, d.start, speaker == null ? -1 : speaker.getId());
        SESSIONS.put(p.getUUID(), s);
        enter(p, s, d.start);
    }

    private static void enter(ServerPlayer p, Session s, String nodeId) {
        Node n = s.d.nodes.get(nodeId);
        if (n == null) { finish(p, s); return; }
        s.node = nodeId;
        if (n.onEnter() != null) n.onEnter().accept(p);
        String name = p.getGameProfile().getName();
        List<String> labels = n.opts().stream().map(o -> o.label().replace("{name}", name)).toList();
        PacketDistributor.sendToPlayer(p, new Network.DialogueOpen(s.entityId, n.speaker(), n.color(), n.text().replace("{name}", name), labels));
    }

    public static void choose(ServerPlayer p, int idx) {
        Session s = SESSIONS.get(p.getUUID());
        if (s == null) return;
        if (idx < 0) {
            SESSIONS.remove(p.getUUID());
            if (s.d.onAbort != null) s.d.onAbort.accept(p);
            return;
        }
        Node n = s.d.nodes.get(s.node);
        if (n == null || idx >= n.opts().size()) return;
        Opt o = n.opts().get(idx);
        if (o.next() == null) finish(p, s);
        else enter(p, s, o.next());
    }

    private static void finish(ServerPlayer p, Session s) {
        SESSIONS.remove(p.getUUID());
        PacketDistributor.sendToPlayer(p, new Network.DialogueClose());
        if (s.d.onEnd != null) s.d.onEnd.accept(p);
    }

    public static void forget(ServerPlayer p) { SESSIONS.remove(p.getUUID()); }
}
