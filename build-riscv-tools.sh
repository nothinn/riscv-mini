# Fetch tools for priv 1.7
git clone https://github.com/chipsalliance/rocket-tools.git riscv-tools-priv1.7
cd riscv-tools-priv1.7
git submodule update --init --recursive

# Build priv 1.7 RISC-V tools
source build.common

echo "Starting RISC-V Toolchain build process"

./build-rv32ima.sh

#build_project riscv-fesvr --prefix=$RISCV
#build_project riscv-gnu-toolchain --prefix=$RISCV --with-xlen=32 --with-arch=rv32ima --with-abi=ilp32
#$MAKE -C riscv-tests/isa        RISCV_PREFIX=riscv32-unknown-elf- XLEN=32
#$MAKE -C riscv-tests/benchmarks RISCV_PREFIX=riscv32-unknown-elf- XLEN=32
