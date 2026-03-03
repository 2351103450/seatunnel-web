import { Col, Row } from "antd";
import React, {
  CSSProperties,
  forwardRef,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import LoginPanel from "./LoginPanel";

/** ===================== Layout (center group) ===================== */
const LAYOUT = {
  absLeft: { orange: 34, blue: 150, black: 325, yellow: 405 } as const,
  absBottom: { orange: 18, blue: 18, black: 22, yellow: 18 } as const,
};

const GROUP_SCALE = 1.4; // 例如 0.9 变小，1.1 变大
const S = (n: number) => Math.round(n * GROUP_SCALE);

/** ===================== Types ===================== */
type Pt = { x: number; y: number };
type ActionType =
  | "BLINK"
  | "SMILE"
  | "SURPRISE"
  | "TILT"
  | "SHAKE"
  | "BOW"
  | "THANKS";
type GlobalAction = { type: ActionType; nonce: number };
type Expression = "idle" | "smile" | "surprise";
type Variant = "orange" | "blue" | "black" | "yellow";
type EntryMode = "bottomBounce" | "topDrop" | "sideSlide";

type CharacterSpec = {
  w: number;
  h: number;
  bg: string;
  radius: number;

  faceTop: number;
  eyeGap: number;
  eyeSize: number;
  pupilSize: number;
  pupilMaxR: number;
  mouthTop: number;

  tiltMul: number;
  shakeMul: number;
  bowMul: number;

  shape: "rect" | "pill" | "semiTop";

  // small personality:
  actionDelayBaseMs: number; // each action gets a small delay so they aren't perfectly synced
  actionAmp: number; // slight amplitude personality
};

function computeLayout() {
  const absL = LAYOUT.absLeft;
  const absB = LAYOUT.absBottom;

  const minLeft = Math.min(...(Object.values(absL) as number[]));

  const relLeft: Record<Variant, number> = {
    orange: absL.orange - minLeft,
    blue: absL.blue - minLeft,
    black: absL.black - minLeft,
    yellow: absL.yellow - minLeft,
  };

  const groupW =
    Math.max(
      absL.orange + SPECS.orange.w,
      absL.blue + SPECS.blue.w,
      absL.black + SPECS.black.w,
      absL.yellow + SPECS.yellow.w
    ) - minLeft;

  // 组高度：max(bottom + height) - min(bottom)（以“组的最低点”为基准）
  const minBottom = Math.min(...(Object.values(absB) as number[]));

  const relBottom: Record<Variant, number> = {
    orange: absB.orange - minBottom,
    blue: absB.blue - minBottom,
    black: absB.black - minBottom,
    yellow: absB.yellow - minBottom,
  };

  const groupH = Math.max(
    relBottom.orange + SPECS.orange.h,
    relBottom.blue + SPECS.blue.h,
    relBottom.black + SPECS.black.h,
    relBottom.yellow + SPECS.yellow.h
  );

  return { relLeft, groupW, relBottom, groupH };
}

const SPECS: Record<Variant, CharacterSpec> = {
  orange: {
    w: S(190),
    h: S(110),
    bg: "#FF8B2B",
    radius: 999,
    faceTop: 34,
    eyeGap: 18,
    eyeSize: 18,
    pupilSize: 7,
    pupilMaxR: 5,
    mouthTop: 70,
    tiltMul: 0.75,
    shakeMul: 0.85,
    bowMul: 0.55,
    shape: "semiTop",
    actionDelayBaseMs: 0,
    actionAmp: 0.95,
  },
  blue: {
    w: S(150),
    h: S(290),
    bg: "#5B3BFF",
    radius: 12,
    faceTop: 42,
    eyeGap: 18,
    eyeSize: 18,
    pupilSize: 7,
    pupilMaxR: 5,
    mouthTop: 86,
    tiltMul: 1.0,
    shakeMul: 1.0,
    bowMul: 1.0,
    shape: "rect",
    actionDelayBaseMs: 30,
    actionAmp: 1.0,
  },
  black: {
    w: S(84),
    h: S(215),
    bg: "#14161C",
    radius: 12,
    faceTop: 38,
    eyeGap: 16,
    eyeSize: 16,
    pupilSize: 6,
    pupilMaxR: 4,
    mouthTop: 78,
    tiltMul: 0.9,
    shakeMul: 1.15,
    bowMul: 0.9,
    shape: "rect",
    actionDelayBaseMs: 55,
    actionAmp: 1.1,
  },
  yellow: {
    w: S(110),
    h: S(185),
    bg: "#FFD33D",
    radius: 999,
    faceTop: 48,
    eyeGap: 18,
    eyeSize: 18,
    pupilSize: 7,
    pupilMaxR: 5,
    mouthTop: 92,
    tiltMul: 0.9,
    shakeMul: 0.95,
    bowMul: 0.95,
    shape: "pill",
    actionDelayBaseMs: 15,
    actionAmp: 0.98,
  },
};

/** ===================== Entry Config (change this!) ===================== */
const ENTRY: Record<
  Variant,
  { mode: EntryMode; delayMs: number; dist?: number }
> = {
  // 你可以随意调整顺序/方式
  orange: { mode: "bottomBounce", delayMs: 0, dist: S(220) },
  blue: { mode: "bottomBounce", delayMs: 80, dist: S(240) },
  black: { mode: "topDrop", delayMs: 160, dist: S(220) },
  yellow: { mode: "sideSlide", delayMs: 240, dist: S(260) },
};

/** ===================== Utils ===================== */
function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n));
}

// smooth-ish step
function smooth01(t: number) {
  const x = clamp(t, 0, 1);
  return x * x * (3 - 2 * x);
}

// a bouncy curve 0..1 -> 0..1
function bounceOut(t: number) {
  let x = clamp(t, 0, 1);
  const n1 = 7.5625;
  const d1 = 2.75;
  if (x < 1 / d1) return n1 * x * x;
  if (x < 2 / d1) return n1 * (x -= 1.5 / d1) * x + 0.75;
  if (x < 2.5 / d1) return n1 * (x -= 2.25 / d1) * x + 0.9375;
  return n1 * (x -= 2.625 / d1) * x + 0.984375;
}

function vecToPupilOffset(from: Pt, to: Pt, maxR: number) {
  const dx = to.x - from.x;
  const dy = to.y - from.y;
  const len = Math.hypot(dx, dy) || 1;
  const ux = dx / len;
  const uy = dy / len;
  return { x: ux * maxR, y: uy * maxR };
}

function getCenter(el: HTMLElement | null): Pt | null {
  if (!el) return null;
  const r = el.getBoundingClientRect();
  return { x: r.left + r.width / 2, y: r.top + r.height / 2 };
}

// deterministic tiny jitter based on nonce + key
function hash01(nonce: number, key: string) {
  let h = 2166136261;
  const s = `${nonce}:${key}`;
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  // 0..1
  return ((h >>> 0) % 1000) / 1000;
}

/** ===================== Character ===================== */
function Character(props: {
  variant: Variant;
  mouse: Pt;
  action: GlobalAction;
  globalTilt: number;
  bootT: number; // global clock (ms since mount)
  style?: CSSProperties;
}) {
  const { variant, mouse, action, globalTilt, bootT, style } = props;
  const spec = SPECS[variant];
  const entry = ENTRY[variant];

  const eyeLeftRef = useRef<HTMLDivElement | null>(null);
  const eyeRightRef = useRef<HTMLDivElement | null>(null);

  const [expression, setExpression] = useState<Expression>("idle");
  const [blink, setBlink] = useState(false);

  // local transforms
  const [tilt, setTilt] = useState(0);
  const [bow, setBow] = useState(0);

  // timers management
  const timersRef = useRef<number[]>([]);
  const clearTimers = () => {
    timersRef.current.forEach((t) => window.clearTimeout(t));
    timersRef.current = [];
  };
  const runAction = (fn: () => void) => {
    clearTimers();
    fn();
  };

  // pupils follow mouse
  const offsets = useMemo(() => {
    const cL = getCenter(eyeLeftRef.current);
    const cR = getCenter(eyeRightRef.current);

    const left = cL
      ? vecToPupilOffset(cL, mouse, spec.pupilMaxR)
      : { x: 0, y: 0 };
    const right = cR
      ? vecToPupilOffset(cR, mouse, spec.pupilMaxR)
      : { x: 0, y: 0 };

    return {
      left: {
        x: clamp(left.x, -spec.pupilMaxR, spec.pupilMaxR),
        y: clamp(left.y, -spec.pupilMaxR, spec.pupilMaxR),
      },
      right: {
        x: clamp(right.x, -spec.pupilMaxR, spec.pupilMaxR),
        y: clamp(right.y, -spec.pupilMaxR, spec.pupilMaxR),
      },
    };
  }, [mouse, spec.pupilMaxR]);

  /** ===== Entry animation (per-character mode + delay) ===== */
  const dist = entry.dist ?? 220;
  const enterDur = 650; // ms
  const t01 = (bootT - entry.delayMs) / enterDur;
  const p = clamp(t01, 0, 1);
  const b = bounceOut(p); // bouncy position

  // position offsets
  let enterX = 0;
  let enterY = 0;
  if (entry.mode === "bottomBounce") {
    enterY = (1 - b) * dist;
  } else if (entry.mode === "topDrop") {
    enterY = -(1 - b) * dist;
  } else if (entry.mode === "sideSlide") {
    enterX = -(1 - b) * dist;
  }

  // scale pop near the end
  const pop = smooth01((p - 0.72) / 0.28); // 0..1 late
  const enterScale = 1 + 0.05 * pop * (1 - (p > 0.95 ? (p - 0.95) / 0.05 : 0)); // slight pop then settle

  /** ===== Shadow (bigger when in-air, smaller when landed) ===== */
  // When enterY is large => in air => shadow larger and lighter.
  // Convert "height" to 0..1
  const height01 = clamp(Math.abs(enterY) / dist, 0, 1);
  const shadowScaleX = 1 + height01 * 0.9;
  const shadowOpacity = 0.22 + (1 - height01) * 0.18; // darker near ground
  const shadowBlur = 10 + height01 * 10;

  /** ===== Actions ===== */
  const doBlink = () =>
    runAction(() => {
      setBlink(true);
      timersRef.current.push(window.setTimeout(() => setBlink(false), 140));
    });

  const doSurprise = () =>
    runAction(() => {
      setExpression("surprise");
      timersRef.current.push(
        window.setTimeout(() => setExpression("idle"), 900)
      );
    });

  const doSmile = () =>
    runAction(() => {
      setExpression("smile");
      timersRef.current.push(
        window.setTimeout(() => setExpression("idle"), 1200)
      );
    });

  const doTilt = (amp: number) =>
    runAction(() => {
      setTilt(-10 * spec.tiltMul * amp);
      timersRef.current.push(
        window.setTimeout(() => setTilt(10 * spec.tiltMul * amp), 160)
      );
      timersRef.current.push(
        window.setTimeout(() => setTilt(-6 * spec.tiltMul * amp), 320)
      );
      timersRef.current.push(window.setTimeout(() => setTilt(0), 520));
    });

  const doShakeHead = (amp: number) =>
    runAction(() => {
      const A = 12 * spec.shakeMul * amp;
      setTilt(-A);
      timersRef.current.push(window.setTimeout(() => setTilt(A), 120));
      timersRef.current.push(window.setTimeout(() => setTilt(-A * 0.85), 240));
      timersRef.current.push(window.setTimeout(() => setTilt(A * 0.85), 360));
      timersRef.current.push(window.setTimeout(() => setTilt(0), 520));
    });

  const doBow = (amp: number) =>
    runAction(() => {
      const B = 1 * spec.bowMul * amp;
      setBow(B);
      timersRef.current.push(window.setTimeout(() => setBow(B * 0.6), 120));
      timersRef.current.push(window.setTimeout(() => setBow(B * 0.9), 240));
      timersRef.current.push(window.setTimeout(() => setBow(0), 520));
    });

  const doThanks = (amp: number) =>
    runAction(() => {
      setExpression("smile");
      setBlink(true);
      setBow(1 * spec.bowMul * amp);

      timersRef.current.push(window.setTimeout(() => setBlink(false), 140));
      timersRef.current.push(window.setTimeout(() => setBow(0), 520));
      timersRef.current.push(
        window.setTimeout(() => setExpression("idle"), 900)
      );
    });

  // respond to global action with per-character delay + amplitude jitter
  useEffect(() => {
    const j = hash01(action.nonce, variant); // 0..1
    const extraDelay = 0; // 0..70ms
    const delay = 1;

    const amp = spec.actionAmp * (0.92 + j * 0.16); // ~0.92..1.08

    const t = window.setTimeout(() => {
      switch (action.type) {
        case "BLINK":
          doBlink();
          break;
        case "SMILE":
          doSmile();
          break;
        case "SURPRISE":
          doSurprise();
          break;
        case "TILT":
          doTilt(amp);
          break;
        case "SHAKE":
          doShakeHead(amp);
          break;
        case "BOW":
          doBow(amp);
          break;
        case "THANKS":
          doThanks(amp);
          break;
      }
    }, delay);

    return () => window.clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [action.nonce]);

  /** ===== combine transforms ===== */
  const global = globalTilt * 0.65;
  const bodyTransform = `
    translateX(${enterX}px)
    translateY(${enterY}px)
    scale(${enterScale})
    rotate(${global + tilt}deg)
    translateY(${bow * 10}px)
    scaleY(${1 - bow * 0.06})
  `;

  /** ===== Pieces ===== */
  const eyeStyle: React.CSSProperties = {
    width: spec.eyeSize,
    height: blink ? Math.max(4, Math.round(spec.eyeSize * 0.25)) : spec.eyeSize,
    borderRadius: 999,
    background: "#fff",
    position: "relative",
    overflow: "hidden",
    transition: "height 120ms ease",
  };

  const pupilStyleBase: React.CSSProperties = {
    width: spec.pupilSize,
    height: spec.pupilSize,
    borderRadius: 999,
    background: "#111",
    position: "absolute",
    left: "50%",
    top: "50%",
    opacity: blink ? 0 : 1,
    transition: "transform 60ms linear, opacity 80ms ease",
  };

  const mouthColor =
    variant === "black" ? "rgba(255,255,255,0.45)" : "rgba(0,0,0,0.35)";

  const mouthNode = (() => {
    if (expression === "surprise") {
      return (
        <div
          style={{
            position: "absolute",
            top: spec.mouthTop,
            left: "50%",
            transform: "translateX(-50%)",
            width: 10,
            height: 14,
            borderRadius: 999,
            background: mouthColor,
            transition: "all 180ms ease",
          }}
        />
      );
    }
    if (expression === "smile") {
      return (
        <div
          style={{
            position: "absolute",
            top: spec.mouthTop,
            left: "50%",
            transform: "translateX(-50%)",
            width: 22,
            height: 12,
            borderBottomLeftRadius: 999,
            borderBottomRightRadius: 999,
            borderTopLeftRadius: 10,
            borderTopRightRadius: 10,
            border: `3px solid ${mouthColor}`,
            borderTop: "0px solid transparent",
            background: "transparent",
            transition: "all 180ms ease",
          }}
        />
      );
    }
    return (
      <div
        style={{
          position: "absolute",
          top: spec.mouthTop,
          left: "50%",
          transform: "translateX(-50%)",
          width: 18,
          height: 6,
          borderRadius: 999,
          background: mouthColor,
          transition: "all 180ms ease",
        }}
      />
    );
  })();

  const faceLayer: CSSProperties = {
    position: "absolute",
    top: spec.faceTop,
    left: 0,
    right: 0,
    display: "flex",
    justifyContent: "center",
    gap: spec.eyeGap,
    pointerEvents: "none",
  };

  const transition = "transform 420ms cubic-bezier(0.22, 1.4, 0.36, 1)";

  /** ===== Body by shape ===== */
  if (spec.shape === "semiTop") {
    // circular top, flat bottom (show top half of a full circle)
    const wrap: CSSProperties = {
      width: spec.w,
      height: spec.h,
      position: "relative",
      overflow: "hidden",
      borderRadius: 12,
      transform: bodyTransform,
      transformOrigin: "50% 85%",
      transition,
      ...(style || {}),
    };

    const circle: CSSProperties = {
      position: "absolute",
      left: 0,
      top: 0,
      width: spec.w,
      height: spec.h * 2,
      borderRadius: 999,
      background: spec.bg,
      boxShadow: "0 10px 25px rgba(0,0,0,0.10)",
    };

    return (
      <div style={wrap}>
        <div style={circle} />
        <div style={faceLayer}>
          <div ref={eyeLeftRef} style={eyeStyle}>
            <div
              style={{
                ...pupilStyleBase,
                transform: `translate(-50%, -50%) translate(${offsets.left.x}px, ${offsets.left.y}px)`,
              }}
            />
          </div>
          <div ref={eyeRightRef} style={eyeStyle}>
            <div
              style={{
                ...pupilStyleBase,
                transform: `translate(-50%, -50%) translate(${offsets.right.x}px, ${offsets.right.y}px)`,
              }}
            />
          </div>
        </div>
        {mouthNode}
      </div>
    );
  }

  const body: CSSProperties = {
    width: spec.w,
    height: spec.h,
    background: spec.bg,
    borderRadius: spec.radius,
    position: "relative",
    boxShadow: "0 10px 25px rgba(0,0,0,0.10)",
    transform: bodyTransform,
    transformOrigin: "50% 85%",
    transition,
    ...(style || {}),
  };

  return (
    <div style={body}>
      <div style={faceLayer}>
        <div ref={eyeLeftRef} style={eyeStyle}>
          <div
            style={{
              ...pupilStyleBase,
              transform: `translate(-50%, -50%) translate(${offsets.left.x}px, ${offsets.left.y}px)`,
            }}
          />
        </div>
        <div ref={eyeRightRef} style={eyeStyle}>
          <div
            style={{
              ...pupilStyleBase,
              transform: `translate(-50%, -50%) translate(${offsets.right.x}px, ${offsets.right.y}px)`,
            }}
          />
        </div>
      </div>
      {mouthNode}
    </div>
  );
}

/** ===================== Scene ===================== */
const CharactersScene = forwardRef<
  HTMLDivElement,
  {
    mouse: Pt;
    action: GlobalAction;
    globalTilt: number;
    bootT: number;
    stageW: number;
    stageH: number;
  }
>(({ mouse, action, globalTilt, bootT, stageW, stageH }, ref) => {
  const { relLeft, groupW, relBottom, groupH } = useMemo(
    () => computeLayout(),
    []
  );
  const baseLeft = Math.max(0, (stageW - groupW) / 2);
  const baseBottom = Math.max(0, (stageH - groupH) / 2);
  return (
    <div
      ref={ref}
      style={{
        width: "100%",
        height: "100vh",
        background: "#f6f6f8",
        borderRadius: 16,
        position: "relative",
        overflow: "hidden",
      }}
    >
      {/* orange semi: front-most */}
      <Character
        variant="orange"
        mouse={mouse}
        action={action}
        globalTilt={globalTilt}
        bootT={bootT}
        style={{
          position: "absolute",
          left: baseLeft + relLeft.orange,
          bottom: baseBottom + relBottom.orange,
          zIndex: 30,
        }}
      />

      <Character
        variant="blue"
        mouse={mouse}
        action={action}
        globalTilt={globalTilt}
        bootT={bootT}
        style={{
          position: "absolute",
          left: baseLeft + relLeft.blue,
          bottom: baseBottom + relBottom.blue,
          zIndex: 10,
        }}
      />

      <Character
        variant="black"
        mouse={mouse}
        action={action}
        globalTilt={globalTilt}
        bootT={bootT}
        style={{
          position: "absolute",
          left: baseLeft + relLeft.black,
          bottom: baseBottom + relBottom.black,
          zIndex: 12,
        }}
      />

      <Character
        variant="yellow"
        mouse={mouse}
        action={action}
        globalTilt={globalTilt}
        bootT={bootT}
        style={{
          position: "absolute",
          left: baseLeft + relLeft.yellow,
          bottom: baseBottom + relBottom.yellow,
          zIndex: 11,
        }}
      />
    </div>
  );
});
CharactersScene.displayName = "CharactersScene";

/** ===================== Page ===================== */
export default function BlueCrewDemo() {
  const stageRef = useRef<HTMLDivElement | null>(null);

  const [mouse, setMouse] = useState<Pt>({ x: 0, y: 0 });
  const [action, setAction] = useState<GlobalAction>({
    type: "BLINK",
    nonce: 0,
  });

  // global body tilt follows mouse
  const [globalTilt, setGlobalTilt] = useState(0);

  // global time for coordinated entries
  const [bootT, setBootT] = useState(0);
  const bootStartRef = useRef<number>(0);

  useEffect(() => {
    bootStartRef.current = performance.now();
    let raf = 0;

    const tick = () => {
      const now = performance.now();
      setBootT(now - bootStartRef.current);
      raf = requestAnimationFrame(tick);
    };

    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, []);

  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      setMouse({ x: e.clientX, y: e.clientY });

      const el = stageRef.current;
      if (!el) return;

      const r = el.getBoundingClientRect();
      const cx = r.left + r.width / 2;

      const dx = (e.clientX - cx) / (r.width / 2); // -1..1
      const maxDeg = 6;
      const target = clamp(dx, -1, 1) * maxDeg;

      // damping
      setGlobalTilt((prev) => prev * 0.85 + target * 0.15);
    };

    window.addEventListener("mousemove", onMove, { passive: true });
    return () => window.removeEventListener("mousemove", onMove);
  }, []);

  const [stageSize, setStageSize] = useState({ w: 0, h: 0 });

  useEffect(() => {
    const el = stageRef.current;
    if (!el) return;

    const update = () => {
      const r = el.getBoundingClientRect();
      setStageSize({ w: r.width, h: r.height });
    };

    const ro = new ResizeObserver(update);
    ro.observe(el);
    update();

    return () => ro.disconnect();
  }, []);

  const fire = (type: ActionType) => setAction({ type, nonce: Date.now() });

  return (
    <Row gutter={24} style={{ margin: 0, padding: 0 }}>
      <Col span={18} style={{ margin: 0, padding: 0 }}>
        <CharactersScene
          ref={stageRef}
          mouse={mouse}
          action={action}
          globalTilt={globalTilt}
          bootT={bootT}
          stageW={stageSize.w}
          stageH={stageSize.h}
        />
      </Col>

      <Col
        span={6}
        style={{
          display: "flex",
          flexDirection: "column",
          margin: 0,
          padding: 0,
        }}
      >
        <LoginPanel onFire={fire} />
      </Col>
    </Row>
  );
}
