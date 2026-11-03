CREATE TABLE usuarios (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL,
    email     VARCHAR(150) NOT NULL UNIQUE,
    password  VARCHAR(100) NOT NULL,
    rol       VARCHAR(20)  NOT NULL,
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('USER', 'ADMIN'))
);

CREATE TABLE productos (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku           VARCHAR(50)    NOT NULL UNIQUE,
    nombre        VARCHAR(150)   NOT NULL,
    precio        NUMERIC(12, 2) NOT NULL,
    stock         INTEGER        NOT NULL DEFAULT 0,
    stock_minimo  INTEGER        NOT NULL DEFAULT 0,
    activo        BOOLEAN        NOT NULL DEFAULT TRUE,
    -- Control de concurrencia optimista: Hibernate la incrementa en cada UPDATE
    version       BIGINT         NOT NULL DEFAULT 0,
    creado_en     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    -- La base de datos misma impide stock negativo, aunque el código falle
    CONSTRAINT ck_productos_stock    CHECK (stock >= 0),
    CONSTRAINT ck_productos_minimo   CHECK (stock_minimo >= 0),
    CONSTRAINT ck_productos_precio   CHECK (precio >= 0)
);

CREATE TABLE movimientos (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    producto_id       BIGINT       NOT NULL REFERENCES productos (id),
    usuario_id        BIGINT       NOT NULL REFERENCES usuarios (id),
    tipo              VARCHAR(20)  NOT NULL,
    cantidad          INTEGER      NOT NULL,
    -- Stock que quedó después del movimiento: sirve de auditoría
    stock_resultante  INTEGER      NOT NULL,
    motivo            VARCHAR(255),
    fecha             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_movimientos_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE')),
    -- ENTRADA y SALIDA llevan cantidad positiva; el AJUSTE puede ser +/- pero no cero
    CONSTRAINT ck_movimientos_cantidad CHECK (
        (tipo IN ('ENTRADA', 'SALIDA') AND cantidad > 0)
        OR (tipo = 'AJUSTE' AND cantidad <> 0)
    ),
    CONSTRAINT ck_movimientos_resultante CHECK (stock_resultante >= 0)
);

-- Índices para las consultas más frecuentes
CREATE INDEX idx_movimientos_producto_fecha ON movimientos (producto_id, fecha DESC);
CREATE INDEX idx_productos_stock_bajo ON productos (stock, stock_minimo);