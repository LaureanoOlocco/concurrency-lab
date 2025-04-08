import re
import os
import shutil

# ---------------------------------------
# Eliminar todos los _pycache_ y .pyc
# ---------------------------------------
for root, dirs, files in os.walk('.'):
    for d in dirs:
        if d == '_pycache_':
            shutil.rmtree(os.path.join(root, d))
    for f in files:
        if f.endswith('.pyc'):
            os.remove(os.path.join(root, f))

# ---------------------------------------
# Borrar archivo temp si existe antes de empezar
# ---------------------------------------
if os.path.exists('temp.txt'):
    os.remove('temp.txt')


# Leer el contenido del archivo de log original
with open('../log.txt', 'r') as archivo_log:
    contenido_log = archivo_log.readline()

# Asegurarse de eliminar archivo temporal si existe
if os.path.exists('temp.txt'):
    os.remove('temp.txt')

# Guardar una copia del log para el procesamiento
with open('temp.txt', 'w') as copia_log:
    copia_log.write(contenido_log)

contador_invariantes = 0

while True:
    # Leer el archivo que se va depurando con regex
    with open('temp.txt', 'r') as archivo_temp:
        secuencia_transiciones = archivo_temp.readline()

    # Expresión regular que representa las secuencias válidas
    patron_transiciones = (
        r'(T0 )(.*?)(T1 )(.*?)'
        r'((T3 )(.*?)(T4 )(.*?)'
        r'((T7 )(.*?)(T8 )(.*?)|(T6 )(.*?)(T9 )(.*?)(T10 )(.*?))|'
        r'(T2 )(.*?)(T5 )(.*?)'
        r'((T7 )(.*?)(T8 )(.*?)|(T6 )(.*?)(T9 )(.*?)(T10 )(.*?)))'
        r'(T11\s)'
    )

    # Grupos de captura que se van a conservar
    grupos = (
        r'\g<2>\g<4>\g<7>\g<9>\g<12>\g<14>\g<16>'
        r'\g<18>\g<20>\g<22>\g<24>\g<27>\g<29>\g<31>\g<33>\g<35>'
    )

    resultado, cantidad = re.subn(patron_transiciones, grupos, secuencia_transiciones)
    contador_invariantes += cantidad

    print((resultado, cantidad))

    # Actualizar el archivo con el contenido restante
    with open('temp.txt', 'w') as archivo_actualizado:
        archivo_actualizado.write(resultado)

    if cantidad == 0:
        print('\n----------------------------------------------------')
        print('ERROR: quedaron elementos sin analizar:', resultado)
        print('Cantidad total de invariantes detectadas:', contador_invariantes)
        break

    if resultado.strip() == '':
        print('\n----------------------------------------------------')
        print('ÉXITO: análisis completado correctamente.')
        print('Cantidad total de invariantes detectadas:', contador_invariantes)
        break

# Eliminar archivo temporal
if os.path.exists('temp.txt'):
    os.remove('temp.txt')
