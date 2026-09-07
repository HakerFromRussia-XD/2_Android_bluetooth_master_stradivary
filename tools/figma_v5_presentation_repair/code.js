const PAGE_ID = "2169:1317";
const CHASSIS_URL =
  "http://localhost:8765/phone_chassis_landscape_upward_v2_cropped.png";
const V5_Y = 8720;

const sourceIds = [
  "2198:1371",
  "2198:1385",
  "2198:1399"
];

function allDescendants(node) {
  if (!("findAll" in node)) return [];
  return node.findAll(() => true);
}

function collectFonts(nodes) {
  const unique = new Map();
  for (const node of nodes) {
    const texts =
      node.type === "TEXT"
        ? [node]
        : "findAllWithCriteria" in node
          ? node.findAllWithCriteria({ types: ["TEXT"] })
          : [];
    for (const text of texts) {
      for (const segment of text.getStyledTextSegments(["fontName"])) {
        unique.set(JSON.stringify(segment.fontName), segment.fontName);
      }
    }
  }
  return [...unique.values()];
}

function findByName(root, name) {
  if (!("findOne" in root)) return null;
  return root.findOne((node) => node.name === name);
}

function findNameContains(root, fragment) {
  if (!("findOne" in root)) return null;
  return root.findOne((node) => node.name.includes(fragment));
}

function findOrCloneTargetFrames(page, sources) {
  const frames = [];
  for (let offset = 0; offset < 3; offset += 1) {
    const index = offset + 6;
    const expectedX = (index - 1) * 1160;
    let frame = page.children.find(
      (node) =>
        node.type === "FRAME" &&
        node.y >= 8500 &&
        node.y < 11000 &&
        Math.abs(node.x - expectedX) < 8 &&
        Math.abs(node.width - 1080) < 8 &&
        Math.abs(node.height - 1920) < 8
    );

    if (!frame) {
      frame = sources[offset].clone();
      page.appendChild(frame);
      frame.name = frame.name.replace(
        /^V4 WORK/,
        "V5 WORK"
      );
      frame.x = expectedX;
      frame.y = V5_Y;
      frame.setSharedPluginData(
        "codex.presentation",
        "variant",
        "V5-work"
      );
      frame.setSharedPluginData(
        "codex.presentation",
        "sourceId",
        sources[offset].id
      );
    }
    frames.push(frame);
  }

  return frames;
}

function createScreenPlane(name, imageHash) {
  const svg =
    '<svg width="1684" height="560" viewBox="0 0 1684 560" fill="none" xmlns="http://www.w3.org/2000/svg">' +
    '<path d="M205 65 L1479 65 Q1534 76 1558 128 L1610 374 Q1622 423 1550 438 L134 438 Q65 425 78 374 L143 128 Q156 79 205 65 Z" fill="#FFFFFF"/>' +
    "</svg>";
  const plane = figma.createNodeFromSvg(svg);
  plane.name = name;
  plane.fills = [];
  plane.strokes = [];
  plane.clipsContent = false;
  const vector = plane.findOne((node) => node.type === "VECTOR");
  if (!vector || vector.type !== "VECTOR") {
    throw new Error(`SVG vector was not created for ${name}`);
  }
  vector.fills = [
    {
      type: "IMAGE",
      imageHash,
      scaleMode: "FILL"
    }
  ];
  return plane;
}

function ensureLandscapeAssembly(frame, chassisHash, index) {
  const oldAssembly = findByName(
    frame,
    "Phone / Photorealistic landscape assembly"
  );
  if (!oldAssembly || oldAssembly.type !== "FRAME") {
    throw new Error(`Original landscape phone missing in ${frame.name}`);
  }

  const sourceScreen = findNameContains(
    oldAssembly,
    "Screen / Game screenshot"
  );
  if (!sourceScreen || !("fills" in sourceScreen)) {
    throw new Error(`Game screenshot source missing in ${frame.name}`);
  }
  const sourceImagePaint = sourceScreen.fills.find(
    (paint) => paint.type === "IMAGE"
  );
  if (!sourceImagePaint || !sourceImagePaint.imageHash) {
    throw new Error(`Game screenshot image fill missing in ${frame.name}`);
  }

  let assembly = findByName(
    frame,
    "Phone / Upward photorealistic landscape assembly"
  );
  if (!assembly) {
    assembly = figma.createFrame();
    assembly.name = "Phone / Upward photorealistic landscape assembly";
    assembly.resize(1684, 560);
    assembly.fills = [];
    assembly.strokes = [];
    assembly.clipsContent = false;
    assembly.setSharedPluginData(
      "codex.presentation",
      "asset",
      "phone_chassis_landscape_upward_v2_cropped"
    );
    frame.appendChild(assembly);

    const shadow = figma.createRectangle();
    shadow.name = "Phone / Contact shadow";
    shadow.resize(1530, 360);
    shadow.x = 78;
    shadow.y = 145;
    shadow.cornerRadius = 120;
    shadow.fills = [
      {
        type: "SOLID",
        color: { r: 0, g: 0, b: 0 },
        opacity: 0.38
      }
    ];
    shadow.effects = [
      {
        type: "LAYER_BLUR",
        radius: 72,
        visible: true
      }
    ];
    assembly.appendChild(shadow);

    const screen = createScreenPlane(
      "Screen / Game screenshot — editable perspective clip",
      sourceImagePaint.imageHash
    );
    assembly.appendChild(screen);
    screen.x = 0;
    screen.y = 0;

    const chassis = figma.createRectangle();
    chassis.name = "Phone / Photorealistic upward-facing chassis";
    chassis.resize(1684, 560);
    chassis.fills = [
      {
        type: "IMAGE",
        imageHash: chassisHash,
        scaleMode: "FILL"
      }
    ];
    chassis.strokes = [];
    assembly.appendChild(chassis);
    chassis.x = 0;
    chassis.y = 0;

    assembly.rescale(2050 / 1684);
  }

  const placements = [
    { x: -485, y: 495, rotation: -8 },
    { x: -470, y: 500, rotation: -7 },
    { x: -500, y: 485, rotation: -9 }
  ];
  const placement = placements[index - 6];
  assembly.x = placement.x;
  assembly.y = placement.y;
  assembly.rotation = placement.rotation;
  assembly.visible = true;
  oldAssembly.visible = false;

  const badge = findByName(frame, "Feature badge");
  if (badge) {
    badge.x = 70;
    badge.y = 1778;
  }
  const statement = findByName(frame, "Feature statement");
  const note = findByName(frame, "Feature note");
  if (statement) statement.visible = false;
  if (note) note.visible = false;

  return assembly;
}

async function main() {
  const [page, chassisImage, ...sources] = await Promise.all([
    figma.getNodeByIdAsync(PAGE_ID),
    figma.createImageAsync(CHASSIS_URL),
    ...sourceIds.map((id) => figma.getNodeByIdAsync(id))
  ]);

  if (!page || page.type !== "PAGE") {
    throw new Error("Google Play presentation page was not found");
  }
  if (sources.some((node) => !node)) {
    throw new Error("One or more source nodes for V5 are missing");
  }

  await Promise.all(collectFonts(sources).map((font) => figma.loadFontAsync(font)));
  await figma.setCurrentPageAsync(page);

  const frames = findOrCloneTargetFrames(page, sources);
  for (let offset = 0; offset < 3; offset += 1) {
    ensureLandscapeAssembly(
      frames[offset],
      chassisImage.hash,
      offset + 6
    );
  }

  page.selection = frames;
  figma.viewport.scrollAndZoomIntoView(page.selection);

  return { frames };
}

main()
  .then(({ frames }) => {
    const ids = [
      ...frames.map((frame) => frame.id),
      ...frames.flatMap((frame) => allDescendants(frame).map((node) => node.id))
    ];
    console.log("V5 frames 6-8 created/updated", ids);
    figma.closePlugin(
      "Пятый ряд: экраны 6–8 обновлены новым крупным upward-корпусом."
    );
  })
  .catch((error) => {
    console.error(error);
    figma.closePlugin(`V5 error: ${error.message || String(error)}`);
  });
